package dev.paoding.longan.channel.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@ChannelHandler.Sharable
public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    @Resource
    private HttpHandler httpHandler;
    @Resource
    private WebSocketHandler webSocketHandler;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        if (message instanceof FullHttpRequest request) {
            httpHandler.channelRead(ctx, request);
        } else if (message instanceof TextWebSocketFrame frame) {
            webSocketHandler.channelRead(ctx, frame);
        } else if (message instanceof BinaryWebSocketFrame frame) {
            webSocketHandler.channelRead(ctx, frame);
        } else {
            ctx.fireChannelRead(message);
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
