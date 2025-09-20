package dev.paoding.longan.channel.http;

import dev.paoding.longan.core.MethodInvocation;
import dev.paoding.longan.core.Result;
import dev.paoding.longan.service.InternalServerException;
import dev.paoding.longan.service.MethodNotAllowedException;
import dev.paoding.longan.service.MethodNotFoundException;
import dev.paoding.longan.service.ServiceException;
import dev.paoding.longan.util.GsonUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
        uri = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        int i = uri.indexOf("?");
        if (i > 0) {
            return new String[]{uri.substring(0, i), uri.substring(i + 1)};
        } else {
            return new String[]{uri};
        }
    }

    public void channelRead(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
        boolean keepAlive = HttpUtil.isKeepAlive(fullHttpRequest);
        HttpVersion httpVersion = fullHttpRequest.protocolVersion();

        String uri = fullHttpRequest.uri().substring(4);
        String[] array = parseURI(uri);
        String path = array[0];

        MethodInvocation methodInvocation = methodInvocationProvider.get(fullHttpRequest.method(), path);
        HttpRequest httpRequest = new HttpRequestImpl(fullHttpRequest, methodInvocation.getPath());

        ScopedResult scopedResult = handlerInterceptor.preHandle(httpRequest);
        if (scopedResult.isPermitted()) {
            scopedResult.run(() -> {
                String query = null;
                if (array.length > 1) {
                    query = array[1];
                }
                try {
                    Result result = httpServiceInvoker.invokeService(methodInvocation, path, query, fullHttpRequest);
                    Object content = result.getValue();
                    if (content == null) {
                        writeNoContent(ctx, keepAlive, httpVersion);
                    } else {
                        AsciiString contentType = result.getType();
                        if (contentType.equals(APPLICATION_JSON)) {
                            if (content instanceof String) {
                                writeJson(ctx, keepAlive, httpVersion, content.toString());
                            } else {
                                writeJson(ctx, keepAlive, httpVersion, GsonUtils.toJson(content));
                            }
                        } else {
                            if (content instanceof VirtualFile virtualFile) {
                                write(ctx, keepAlive, httpVersion, virtualFile, contentType);
                            } else {
                                write(ctx, keepAlive, httpVersion, HttpResponseStatus.OK, content.toString(), contentType);
                            }
                        }
                    }
                } catch (HttpRequestException e) {
                    logger.info("A HttpRequestException occurred in the request", e);
                    if (APPLICATION_JSON.toString().equals(e.getResponseType())) {
                        writeJson(ctx, keepAlive, httpVersion, e.getHttpResponseStatus(), ExceptionResult.of(e));
                    } else {
                        writeText(ctx, keepAlive, httpVersion, e.getHttpResponseStatus(), e.getMessage());
                    }
                } catch (ServiceException e) {
                    logger.info("A ServiceException occurred in the request", e);
                    handelException(ctx, keepAlive, e.getMethodInvocation(), httpVersion, e.getHttpResponseStatus(), ExceptionResult.of(e), e.getMessage());
                } catch (InternalServerException e) {
                    logger.info("A InternalServerException occurred in the request", e);
                    handelException(ctx, keepAlive, e.getMethodInvocation(), httpVersion, e.getHttpResponseStatus(), ExceptionResult.of(e), e.getMessage());
                } catch (MethodNotFoundException e) {
                    logger.warn(e.getMessage());
                    writeText(ctx, keepAlive, httpVersion, e.getHttpResponseStatus(), e.getMessage());
                } catch (MethodNotAllowedException e) {
                    writeText(ctx, keepAlive, httpVersion, e.getHttpResponseStatus(), e.getMessage());
                } catch (Exception e) {
                    logger.warn("An error occurred in the request", e);
                    writeText(ctx, keepAlive, httpVersion, HttpResponseStatus.INTERNAL_SERVER_ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.codeAsText().toString());
                } finally {
                    handlerInterceptor.afterCompletion();
                }
            });
        } else {
            writeText(ctx, keepAlive, httpVersion, HttpResponseStatus.FORBIDDEN, "Forbidden " + fullHttpRequest.uri() + " is denied");
        }
    }

    private void handelException(ChannelHandlerContext ctx, boolean keepAlive, MethodInvocation methodInvocation, HttpVersion httpVersion, HttpResponseStatus httpResponseStatus,
                                 ExceptionResult exceptionResult, String message) {
        if (methodInvocation == null) {
            writeText(ctx, keepAlive, httpVersion, httpResponseStatus, message);
        }
        if (APPLICATION_JSON.toString().equals(methodInvocation.getResponseType())) {
            writeJson(ctx, keepAlive, httpVersion, httpResponseStatus, exceptionResult);
        } else {
            writeText(ctx, keepAlive, httpVersion, httpResponseStatus, message);
        }
    }

    @Override
    protected void postHandle(HttpResponse httpResponse) {
        handlerInterceptor.postHandle(httpResponse);
    }

}
