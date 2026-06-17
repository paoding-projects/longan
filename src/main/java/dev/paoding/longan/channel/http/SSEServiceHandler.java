package dev.paoding.longan.channel.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
public class SSEServiceHandler extends AbstractServiceHandler {
    public final static AttributeKey<SSEContext> SSE_SESSION_ATTRIBUTE_KEY = AttributeKey.valueOf("SSE_SESSION");
    @Resource
    private SSEListenerHandler sseListenerHandler;
    @Resource
    private HandlerInterceptor handlerInterceptor;
    @Setter
    private ExecutorService executorService;

    public void channelRead(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, String uri) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream")
                .set(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_CACHE)
                .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                .set("Access-Control-Allow-Origin", "*")
                .set("X-Accel-Buffering", "no");

        HttpUtil.setTransferEncodingChunked(response, true);
        ctx.writeAndFlush(response);

        HttpRequest httpRequest = new HttpRequestImpl(fullHttpRequest, parseURI(uri)[0]);
        try {
            ScopedContext scopedContext = handlerInterceptor.preHandle(httpRequest);
            scopedContext.run(() -> {
                open(ctx, httpRequest);
            });
        } catch (Exception e) {
            handleError(ctx, e);
        } finally {
            ctx.channel().closeFuture().addListener(future -> close(ctx));
        }
    }

    public void open(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        SSEContext sseContext = new SSEContext(ctx.channel(), httpRequest.getPath());
        ctx.channel().attr(SSE_SESSION_ATTRIBUTE_KEY).set(sseContext);
        sseListenerHandler.onOpen(sseContext, httpRequest);
    }

    public void close(ChannelHandlerContext ctx) {
        executorService.execute(() -> {
            handlerInterceptor.afterCompletion();
            SSEContext sseContext = ctx.channel().attr(SSE_SESSION_ATTRIBUTE_KEY).get();
            ctx.channel().attr(SSE_SESSION_ATTRIBUTE_KEY).set(null);
            if (sseContext != null) {
                sseContext.destroy();
                sseListenerHandler.onClose(sseContext);
            }
            handlerInterceptor.afterCompletion();
        });
    }

    private void handleError(ChannelHandlerContext ctx, Exception e) {
        switch (e) {
            case HttpRequestException ex -> writeAndFlush(ctx, SSEEvent.of("error", ex.getCode()));
            case null, default -> writeAndFlush(ctx, SSEEvent.of("error", "Internal Server Error"));
        }
    }

    private void writeAndFlush(ChannelHandlerContext ctx, SSEEvent event) {
        ByteBuf buf = Unpooled.wrappedBuffer(event.toBytes());
        ctx.writeAndFlush(buf).addListener(future -> {
            if (!future.isSuccess()) {
                ReferenceCountUtil.safeRelease(buf);
            }
        });
    }

    @Override
    protected void postHandle(dev.paoding.longan.channel.http.HttpResponse httpResponse) {
        handlerInterceptor.postHandle(httpResponse);
    }

}
