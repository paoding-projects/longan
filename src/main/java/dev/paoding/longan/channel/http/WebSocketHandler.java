package dev.paoding.longan.channel.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Component
public class WebSocketHandler {
    private final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    public final static AttributeKey<WebSocketContext> WEB_SOCKET_SESSION_ATTRIBUTE_KEY = AttributeKey.valueOf("WEB_SOCKET_SESSION");
    @Resource
    private WebSocketListenerHandler webSocketListenerHandler;
    @Resource
    private HandlerInterceptor handlerInterceptor;
    @Setter
    private ExecutorService executorService;

    public void channelRead(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        WebSocketContext webSocketContext = ctx.channel().attr(WEB_SOCKET_SESSION_ATTRIBUTE_KEY).get();
        webSocketListenerHandler.onMessage(webSocketContext, frame.text());
    }

    public void channelRead(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        WebSocketContext webSocketContext = ctx.channel().attr(WEB_SOCKET_SESSION_ATTRIBUTE_KEY).get();
        webSocketListenerHandler.onMessage(webSocketContext, frame.content().array());
    }

    public void close(ChannelHandlerContext ctx) {
        executorService.execute(() -> {
            WebSocketContext webSocketContext = ctx.channel().attr(WEB_SOCKET_SESSION_ATTRIBUTE_KEY).get();
            ctx.channel().attr(WEB_SOCKET_SESSION_ATTRIBUTE_KEY).set(null);
            if (webSocketContext != null) {
                webSocketListenerHandler.onClose(webSocketContext);
            }
            handlerInterceptor.afterCompletion();
        });
    }

    public void open(ChannelHandlerContext ctx, String requestUri, HttpHeaders httpHeaders) {
        executorService.execute(() -> {
            WebSocketRequestImpl webSocketRequest = new WebSocketRequestImpl(requestUri, httpHeaders);
            try {
                handlerInterceptor.preHandle(webSocketRequest).run(() -> {
                    WebSocketContext webSocketContext = new WebSocketContext(ctx.channel(), requestUri);
                    ctx.channel().attr(WEB_SOCKET_SESSION_ATTRIBUTE_KEY).set(webSocketContext);
                    webSocketListenerHandler.onOpen(webSocketContext, webSocketRequest);
                });
            } catch (Exception e) {
                logger.error("Failed to process WebSocket request at {}", requestUri, e);
                ctx.close();
            }
        });
    }
}
