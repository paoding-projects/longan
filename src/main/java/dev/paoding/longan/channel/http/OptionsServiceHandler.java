package dev.paoding.longan.channel.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.springframework.stereotype.Component;

@Component
public class OptionsServiceHandler extends AbstractServiceHandler {

    public void channelRead(ChannelHandlerContext ctx, FullHttpRequest request) {
        boolean keepAlive = HttpUtil.isKeepAlive(request);

        String origin = request.headers().get(HttpHeaderNames.ORIGIN);
        if (origin == null || origin.isBlank()) {
            DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.NO_CONTENT);
            HttpHeaders httpHeaders = fullHttpResponse.headers();
            httpHeaders.set(HttpHeaderNames.ALLOW, "GET, POST");
            httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);

            HttpResponse httpResponse = new HttpResponseImpl(fullHttpResponse);
            writeAndFlush(ctx, keepAlive, httpResponse);
        } else {
            DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.FORBIDDEN);
            HttpHeaders httpHeaders = fullHttpResponse.headers();
            httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);

            HttpResponse httpResponse = new HttpResponseImpl(fullHttpResponse);
            writeAndFlush(ctx, keepAlive, httpResponse);
        }

    }
}
