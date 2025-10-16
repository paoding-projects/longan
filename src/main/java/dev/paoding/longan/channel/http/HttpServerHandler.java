package dev.paoding.longan.channel.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


@Component
@ChannelHandler.Sharable
public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    @Resource
    private HttpHandler httpHandler;
    @Resource
    private WebSocketHandler webSocketHandler;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        switch (message) {
            case FullHttpRequest request -> httpHandler.channelRead(ctx, request);
            case TextWebSocketFrame frame -> webSocketHandler.channelRead(ctx, frame);
            case BinaryWebSocketFrame frame -> webSocketHandler.channelRead(ctx, frame);
            case null, default -> ctx.fireChannelRead(message);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete handshake) {
            webSocketHandler.open(ctx, handshake.requestUri());
        } else if (event instanceof CloseWebSocketFrame) {
            webSocketHandler.close(ctx);
        } else {
            ctx.fireUserEventTriggered(event);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

}
