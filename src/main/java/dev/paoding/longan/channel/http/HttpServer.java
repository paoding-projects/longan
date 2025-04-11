package dev.paoding.longan.channel.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringChannelOption;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Component
public class HttpServer {
    private final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    @Resource
    private ServerChannelInitializer serverChannelInitializer;
    @Value("${longan.http.port:8001}")
    private int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;

    public void startup() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        String name = SystemPropertyUtil.get("os.name").trim();
        String version = SystemPropertyUtil.get("os.version");
        if (IoUring.isAvailable()) {
            logger.info("io_uring supported on {} {} system.", name, version);
            this.bossGroup = new MultiThreadIoEventLoopGroup(IoUringIoHandler.newFactory());
            this.workGroup = new MultiThreadIoEventLoopGroup(IoUringIoHandler.newFactory());
            start(bossGroup, workGroup, IoUringServerSocketChannel.class);
        } else if (Epoll.isAvailable()) {
            logger.info("EPoll supported on {} {} system.", name, version);
            this.bossGroup = new MultiThreadIoEventLoopGroup(EpollIoHandler.newFactory());
            this.workGroup = new MultiThreadIoEventLoopGroup(EpollIoHandler.newFactory());
            start(bossGroup, workGroup, EpollServerSocketChannel.class);
        } else if (KQueue.isAvailable()) {
            logger.info("KQueue supported on {} {} system.", name, version);
            this.bossGroup = new MultiThreadIoEventLoopGroup(KQueueIoHandler.newFactory());
            this.workGroup = new MultiThreadIoEventLoopGroup(KQueueIoHandler.newFactory());
            start(bossGroup, workGroup, KQueueServerSocketChannel.class);
        } else {
            logger.info("NIO supported on {} {} system.", name, version);
            startNio();
        }
    }

    private void startNio() throws Exception {
        this.bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        this.workGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workGroup).channel(NioServerSocketChannel.class)
                .childHandler(serverChannelInitializer)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture future = bootstrap.bind(port).sync();
        if (!future.isSuccess()) {
            throw new Exception(String.format("Fail to bind on port = %d.", port), future.cause());
        }
        logger.info("Starting server at port {}.", port);
    }

    private void start(EventLoopGroup bossGroup, EventLoopGroup workerGroup, Class<? extends ServerSocketChannel> channelClass) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(channelClass)
                .childHandler(serverChannelInitializer)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(512 * 1024, 1024 * 1024))
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, false);

        if (IoUring.isAvailable()) {
            bootstrap.option(IoUringChannelOption.SO_REUSEADDR, true)
                    .childOption(IoUringChannelOption.TCP_NODELAY, true)
                    .childOption(IoUringChannelOption.SO_KEEPALIVE, true);
        } else if (Epoll.isAvailable()) {
            bootstrap.option(EpollChannelOption.IP_FREEBIND, false)
                    .option(EpollChannelOption.IP_TRANSPARENT, false)
                    .childOption(EpollChannelOption.TCP_CORK, false)
                    .childOption(EpollChannelOption.TCP_QUICKACK, true)
                    .childOption(EpollChannelOption.IP_TRANSPARENT, false);
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