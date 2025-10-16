package dev.paoding.longan.channel.http;

import dev.paoding.longan.core.MethodInvocation;
import dev.paoding.longan.core.Result;
import dev.paoding.longan.service.InternalServerException;
import dev.paoding.longan.service.MethodNotAllowedException;
import dev.paoding.longan.service.MethodNotFoundException;
import dev.paoding.longan.service.ServiceException;
import dev.paoding.longan.util.GsonUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

@Component
public class ApiServiceHandler extends AbstractServiceHandler {
    private final Logger logger = LoggerFactory.getLogger(ApiServiceHandler.class);
    @Resource
    private HandlerInterceptor handlerInterceptor;
    @Resource
    private HttpServiceInvoker httpServiceInvoker;
    @Resource
    private MethodInvocationProvider methodInvocationProvider;

    private String[] parseURI(String uri) {
        int i = uri.indexOf("?");
        if (i < 0) {
            return new String[]{uri};
        } else {
            uri = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            return new String[]{uri.substring(0, i), uri.substring(i + 1)};
        }
    }

    public void channelRead(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, String uri) {
        String[] array = parseURI(uri);
        String path = array[0];

        MethodInvocation methodInvocation = methodInvocationProvider.get(fullHttpRequest.method(), path);
        if (methodInvocation == null) {
            writeNotFound(ctx, fullHttpRequest);
            return;
        }

        HttpRequest httpRequest = new HttpRequestImpl(fullHttpRequest, methodInvocation.getPath());

        try {
            ScopedResult scopedResult = handlerInterceptor.preHandle(httpRequest);
            if (scopedResult.isPermitted()) {
                scopedResult.run(() -> {
                    String query = array.length == 2 ? array[1] : null;
                    Result result = httpServiceInvoker.invokeService(methodInvocation, path, query, fullHttpRequest);
                    Object content = result.getValue();
                    if (content == null) {
                        writeNoContent(ctx, fullHttpRequest);
                    } else if (content instanceof HttpCookie httpCookie) {
                        writeCookie(ctx, fullHttpRequest, httpCookie);
                    } else {
                        AsciiString contentType = result.getType();
                        if (contentType.equals(APPLICATION_JSON)) {
                            if (content instanceof String) {
                                writeJson(ctx, fullHttpRequest, content.toString());
                            } else {
                                writeJson(ctx, fullHttpRequest, GsonUtils.toJson(content));
                            }
                        } else {
                            if (content instanceof VirtualFile virtualFile) {
                                write(ctx, fullHttpRequest, virtualFile, contentType);
                            } else {
                                write(ctx, fullHttpRequest, HttpResponseStatus.OK, content.toString(), contentType);
                            }
                        }
                    }
                });
            } else {
                writeText(ctx, fullHttpRequest, HttpResponseStatus.FORBIDDEN, "Forbidden " + fullHttpRequest.uri() + " is denied");
            }
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
    protected void postHandle(HttpResponse httpResponse) {
        handlerInterceptor.postHandle(httpResponse);
    }

}
