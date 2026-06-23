package dev.paoding.longan.channel.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.*;
import io.netty.util.internal.SystemPropertyUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class HttpServer {
    private final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    @Resource
    private ServerChannelInitializer serverChannelInitializer;
    @Value("${longan.http.port:8001}")
    private int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private static final short BUF_GROUP_ID = 0;

    public void startup() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        String name = SystemPropertyUtil.get("os.name").trim();
        String version = SystemPropertyUtil.get("os.version");
        int processors = Runtime.getRuntime().availableProcessors();
        int bossThreads = 1;
        if (IoUring.isAvailable()) {
            logger.info("io_uring supported on {} {} system.", name, version);
            logger.info("BufferRing supported: {}", IoUring.isRegisterBufferRingSupported());
            //大包场景内核自动跨多个 buffer 接收，减少拼包次数，Netty 检测到支持后自动启用，不需要改代码。
            logger.info("Incremental BufferRing supported: {}", IoUring.isRegisterBufferRingIncSupported());

            IoUringIoHandlerConfig handlerConfig = new IoUringIoHandlerConfig();

            // 每个线程同时飞行的 I/O 操作数上限
            handlerConfig.setRingSize(4096);

            // multishot 模式下 CQE 产生速度远超 SQE，放大 4 倍避免 CQ overflow
            boolean multishotActive = IoUring.isAcceptMultishotEnabled() || IoUring.isRecvMultishotEnabled();
            if (multishotActive) {
                handlerConfig.setCqSize(4096 * 4);
            }

            if (IoUring.isRegisterBufferRingSupported()) {
                IoUringBufferRingConfig bufferRingConfig = IoUringBufferRingConfig.builder()
                        .bufferGroupId(BUF_GROUP_ID)          // buffer group ID，channel 上要用同一个 ID
                        .bufferRingSize((short) 4096)   // buffer 槽数量，高并发下要足够多
                        .batchSize(2048)                // 每批预分配数量，bufferRingSize 的一半
                        .batchAllocation(true)          // 批量预分配，减少运行时分配开销
                        .allocator(new IoUringAdaptiveBufferRingAllocator(
                                PooledByteBufAllocator.DEFAULT,
                                256,    // 初始 buffer 大小
                                1024,   // 初始分配大小
                                65536   // 最大 buffer 大小，覆盖绝大多数业务包
                        ))
                        .build();

                handlerConfig.setBufferRingConfig(bufferRingConfig);
            }
            this.bossGroup = new MultiThreadIoEventLoopGroup(bossThreads, IoUringIoHandler.newFactory());
            this.workGroup = new MultiThreadIoEventLoopGroup(processors, IoUringIoHandler.newFactory(handlerConfig));
            start(IoUringServerSocketChannel.class, ChannelType.IO_URING);
        } else if (Epoll.isAvailable()) {
            logger.info("EPoll supported on {} {} system.", name, version);
            this.bossGroup = new MultiThreadIoEventLoopGroup(bossThreads, EpollIoHandler.newFactory());
            this.workGroup = new MultiThreadIoEventLoopGroup(EpollIoHandler.newFactory());
            start(EpollServerSocketChannel.class, ChannelType.EPOLL);
//        } else if (KQueue.isAvailable()) {
//            logger.info("KQueue supported on {} {} system.", name, version);
//            this.bossGroup = new MultiThreadIoEventLoopGroup(bossThreads, KQueueIoHandler.newFactory());
//            this.workGroup = new MultiThreadIoEventLoopGroup(KQueueIoHandler.newFactory());
//            start(KQueueServerSocketChannel.class, ChannelType.KQUEUE);
        } else {
            logger.info("Java NIO supported on {} {} system.", name, version);
            this.bossGroup = new MultiThreadIoEventLoopGroup(bossThreads, NioIoHandler.newFactory());
            this.workGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            startNio();
        }
    }

    private void startNio() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workGroup).channel(NioServerSocketChannel.class)
                .childHandler(serverChannelInitializer)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture future = bootstrap.bind(port).sync();
        if (!future.isSuccess()) {
            throw new Exception(String.format("Fail to bind on port = %d.", port), future.cause());
        }
        logger.info("Starting http server at port {}.", port);
    }

    private void start(Class<? extends ServerSocketChannel> channelClass, ChannelType channelType) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workGroup).channel(channelClass)
                .childHandler(serverChannelInitializer)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, false)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(32 * 1024, 64 * 1024));

        if (channelType.equals(ChannelType.IO_URING)) {
            //指定使用 BufferRing，对应 handlerConfig 里注册的 groupId,不设置的话 BufferRing 虽然注册了但 channel 不会使用，
            // recv 仍走普通模式，BufferRing 的配置就白费了。
            bootstrap.childOption(IoUringChannelOption.IO_URING_BUFFER_GROUP_ID, BUF_GROUP_ID);

            //keepalive 精细控制，替代 SO_KEEPALIVE 的系统级默认值,用这三个参数可以精确控制探测行为，及时踢掉死连接。
            bootstrap.childOption(IoUringChannelOption.TCP_KEEPIDLE, 60);    // 60s 无数据开始探测
            bootstrap.childOption(IoUringChannelOption.TCP_KEEPINTVL, 10);  // 每隔 10s 探测一次
            bootstrap.childOption(IoUringChannelOption.TCP_KEEPCNT, 3);  // 探测 3 次失败则断开

            // 5.14 内核不支持，设置了也无效;发送大数据时跳过用户态到内核态的内存拷贝，直接从用户内存 DMA 到网卡。阈值设 4096 表示小于 4KB
            // 的写操作走普通路径（小包 zero-copy 反而有额外开销），大于 4KB 才走 send_zc。注意需要确认 ulimit -l 足够大，否则会报 -ENOMEM：
            bootstrap.childOption(IoUringChannelOption.IO_URING_WRITE_ZERO_COPY_THRESHOLD, 4096);
        } else if (channelType.equals(ChannelType.EPOLL)) {
            bootstrap.childOption(EpollChannelOption.TCP_CORK, false);
            bootstrap.childOption(EpollChannelOption.TCP_QUICKACK, true);
        }

        ChannelFuture future = bootstrap.bind(port).sync();
        if (!future.isSuccess()) {
            throw new Exception(String.format("Fail to bind on port = %d.", port), future.cause());
        }
        logger.info("Starting http server on port {}.", port);
    }

    public void shutdown() {
        Thread.currentThread().setName("ApplicationShutdownHook");
        logger.info("Stop http server on port {}.", port);
        if (workGroup != null) {
            workGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }


}