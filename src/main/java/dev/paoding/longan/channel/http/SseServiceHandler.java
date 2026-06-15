package dev.paoding.longan.channel.http;

import dev.paoding.longan.core.MethodInvocation;
import dev.paoding.longan.service.InternalServerException;
import dev.paoding.longan.service.MethodNotAllowedException;
import dev.paoding.longan.service.MethodNotFoundException;
import dev.paoding.longan.service.ServiceException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpResponse;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

@Component
public class SseServiceHandler extends AbstractServiceHandler {
    private final Logger logger = LoggerFactory.getLogger(SseServiceHandler.class);
    @Resource
    private HandlerInterceptor handlerInterceptor;
    @Resource
    private HttpServiceInvoker httpServiceInvoker;
    @Resource
    private MethodInvocationProvider methodInvocationProvider;

    public void channelRead(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, String uri) {
        // 2. 建立 SSE 响应头
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream")
                .set(HttpHeaderNames.CACHE_CONTROL,HttpHeaderValues.NO_CACHE)
                .set(HttpHeaderNames.CONNECTION,  HttpHeaderValues.KEEP_ALIVE)
                .set("X-Accel-Buffering", "no");   // 禁用 Nginx 缓冲

        HttpUtil.setTransferEncodingChunked(response, true);
        ctx.writeAndFlush(response);

        String[] array = parseURI(uri);
        String path = array[0];

        MethodInvocation methodInvocation = methodInvocationProvider.get(fullHttpRequest.method(), path);
        if (methodInvocation == null) {
            writeNotFound(ctx, fullHttpRequest);
            return;
        }

        HttpRequest httpRequest = new HttpRequestImpl(fullHttpRequest, methodInvocation.getPath());
        try {
            ScopedContext scopedContext = handlerInterceptor.preHandle(httpRequest);
            scopedContext.run(() -> {
                String query = array.length == 2 ? array[1] : null;
                httpServiceInvoker.invokeService(methodInvocation, path, query, fullHttpRequest);
            });
        } catch (Exception e) {
            handleError(ctx, fullHttpRequest, methodInvocation, e);
        } finally {
            handlerInterceptor.afterCompletion();
        }
    }

    private void handleError(ChannelHandlerContext ctx, FullHttpRequest request, MethodInvocation methodInvocation, Exception e) {
        switch (e) {
            case HttpRequestException ex -> handleHttpRequestException(ctx, request, ex);
            case ServiceException ex -> handelServiceException(ctx, request, ex);
            case InternalServerException ex -> handleInternalServerException(ctx, request, ex);
            case MethodNotAllowedException ex ->
                    handleMethodNotAllowed(ctx, request, methodInvocation.getResponseType(), ex);
            case MethodNotFoundException ex ->
                    handleMethodNotFoundException(ctx, request, methodInvocation.getResponseType(), ex);
            case null, default -> handleException(ctx, request, methodInvocation.getResponseType(), e);
        }
    }

    private void writeNotFound(ChannelHandlerContext ctx, FullHttpRequest request) {
        String message = request.method() + " " + request.uri() + " not found";
        writeText(ctx, request, HttpResponseStatus.NOT_FOUND, message);
    }

    private void handleMethodNotFoundException(ChannelHandlerContext ctx, FullHttpRequest request, String responseType, MethodNotFoundException ex) {
        String message = request.method() + " " + request.uri() + " " + ex.getMessage();
        if (APPLICATION_JSON.toString().equals(responseType)) {
            ExceptionResult exceptionResult = ExceptionResult.of(ex);
            writeJson(ctx, request, ex.getHttpResponseStatus(), exceptionResult);
        } else {
            writeText(ctx, request, ex.getHttpResponseStatus(), message);
        }
    }

    private void handleMethodNotAllowed(ChannelHandlerContext ctx, FullHttpRequest request, String responseType, MethodNotAllowedException ex) {
        String message = request.method() + " " + request.uri() + " " + ex.getMessage();
        if (APPLICATION_JSON.toString().equals(responseType)) {
            ExceptionResult exceptionResult = ExceptionResult.of(ex);
            writeJson(ctx, request, ex.getHttpResponseStatus(), exceptionResult);
        } else {
            writeText(ctx, request, ex.getHttpResponseStatus(), message);
        }
    }

    private void handleException(ChannelHandlerContext ctx, FullHttpRequest request, String responseType, Exception e) {
        logger.error("Failed to handle {} request for {}", request.method(), request.uri(), e);
        if (APPLICATION_JSON.toString().equals(responseType)) {
            ExceptionResult exceptionResult = ExceptionResult.of("internal.server.error", "Internal Server Error");
            writeJson(ctx, request, HttpResponseStatus.INTERNAL_SERVER_ERROR, exceptionResult);
        } else {
            writeText(ctx, request, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    private void handleHttpRequestException(ChannelHandlerContext ctx, FullHttpRequest request, HttpRequestException e) {
        if (APPLICATION_JSON.toString().equals(e.getResponseType())) {
            writeJson(ctx, request, e.getHttpResponseStatus(), ExceptionResult.of(e));
        } else {
            writeText(ctx, request, e.getHttpResponseStatus(), e.getMessage());
        }
    }

    private void handelServiceException(ChannelHandlerContext ctx, FullHttpRequest request, ServiceException e) {
        handelServerException(ctx, request, e.getMethodInvocation(), e.getHttpResponseStatus(), ExceptionResult.of(e), e.getMessage());
    }

    private void handleInternalServerException(ChannelHandlerContext ctx, FullHttpRequest request, InternalServerException e) {
        logger.error("An InternalServerException occurred while processing {} {}",
                request.method(), request.uri(), e);
        handelServerException(ctx, request, e.getMethodInvocation(), e.getHttpResponseStatus(), ExceptionResult.of(e), e.getMessage());
    }

    private void handelServerException(ChannelHandlerContext ctx, FullHttpRequest request, MethodInvocation methodInvocation, HttpResponseStatus httpResponseStatus, ExceptionResult exceptionResult, String message) {
        if (methodInvocation == null) {
            writeText(ctx, request, httpResponseStatus, message);
        } else {
            if (APPLICATION_JSON.toString().equals(methodInvocation.getResponseType())) {
                writeJson(ctx, request, httpResponseStatus, exceptionResult);
            } else {
                writeText(ctx, request, httpResponseStatus, message);
            }
        }
    }

    @Override
    protected void postHandle(dev.paoding.longan.channel.http.HttpResponse httpResponse) {
        handlerInterceptor.postHandle(httpResponse);
    }

}
