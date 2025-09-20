package dev.paoding.longan.channel.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OptionsServiceHandler extends AbstractServiceHandler {
    @Value("${longan.http.cross-origin:false}")
    private Boolean enableCrossOrigin;

    @Override
    public void channelRead(ChannelHandlerContext ctx, FullHttpRequest request) {
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.NO_CONTENT);
        if (enableCrossOrigin) {
            HttpHeaders httpHeaders = fullHttpResponse.headers();
            httpHeaders.set("Access-Control-Allow-Origin", "*");
            httpHeaders.set("Access-Control-Allow-Methods", "*");
            httpHeaders.set("Access-Control-Allow-Headers", "*");
            httpHeaders.set("Access-Control-Allow-Credentials", "true");
        }
        HttpResponse httpResponse = new HttpResponseImpl(fullHttpResponse);
        writeAndFlush(ctx, keepAlive, httpResponse);
    }
}
