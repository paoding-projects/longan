package dev.paoding.longan.channel.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


@Component
@ChannelHandler.Sharable
public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);
    @Resource
    private WebSocketHandler webSocketHandler;
    @Resource
    private ApiServiceHandler apiServiceHandler;
    @Resource
    private SSEServiceHandler sseServiceHandler;
    @Resource
    private DocServiceHandler docServiceHandler;
    @Resource
    private OptionsServiceHandler optionsServiceHandler;
    @Resource
    private NotFoundServiceHandler notFoundServiceHandler;
    private static final String API_PREFIX = "/api/";
    private static final String SSE_PREFIX = "/sse/";
    private static final String DOC_PREFIX = "/doc/";
    private final ExecutorService executorService;

    {
        ThreadFactory threadFactory = Thread.ofVirtual().name("http-thread-", 0).uncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                logger.error(thread.getName(), throwable);
            }
        }).factory();
        executorService = Executors.newThreadPerTaskExecutor(threadFactory);
    }

    @PostConstruct
    public void init() {
        sseServiceHandler.setExecutorService(executorService);
        webSocketHandler.setExecutorService(executorService);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        switch (message) {
            case FullHttpRequest request -> channelRead(ctx, request);
            case TextWebSocketFrame frame -> channelRead(ctx, frame);
            case BinaryWebSocketFrame frame -> channelRead(ctx, frame);
            case null, default -> ctx.fireChannelRead(message);
        }
    }

    private void channelRead(ChannelHandlerContext ctx, FullHttpRequest request) {
        executorService.execute(() -> {
            try {
                if (request.method() == HttpMethod.OPTIONS) {
                    optionsServiceHandler.channelRead(ctx, request);
                } else {
                    String uri = request.uri();
                    if (uri.startsWith(API_PREFIX)) {
                        apiServiceHandler.channelRead(ctx, request, uri.substring(4));
                    } else if (uri.startsWith(SSE_PREFIX)) {
                        sseServiceHandler.channelRead(ctx, request, uri.substring(4));
                    } else if (uri.startsWith(DOC_PREFIX)) {
                        docServiceHandler.channelRead(ctx, request, uri);
                    } else {
                        notFoundServiceHandler.channelRead(ctx, request);
                    }
                }
            } finally {
                ReferenceCountUtil.release(request);
            }
        });
    }

    public void channelRead(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        executorService.execute(() -> {
            try {
                webSocketHandler.channelRead(ctx, frame);
            } finally {
                ReferenceCountUtil.release(frame);
            }
        });
    }

    public void channelRead(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        executorService.execute(() -> {
            try {
                webSocketHandler.channelRead(ctx, frame);
            } finally {
                ReferenceCountUtil.release(frame);
            }
        });
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete handshake) {
            ctx.channel().config().setWriteBufferWaterMark(new WriteBufferWaterMark(128 * 1024, 256 * 1024));
            webSocketHandler.open(ctx, handshake.requestUri().substring(3), handshake.requestHeaders());
            ctx.channel().closeFuture().addListener(future -> webSocketHandler.close(ctx));
        } else {
            ctx.fireUserEventTriggered(event);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();
    }

}
