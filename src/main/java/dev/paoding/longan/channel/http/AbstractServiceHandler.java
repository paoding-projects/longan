package dev.paoding.longan.channel.http;

import dev.paoding.longan.core.ResponseFilter;
import dev.paoding.longan.core.Result;
import dev.paoding.longan.util.JsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static dev.paoding.longan.channel.http.Http.ContentType.APPLICATION_JAVASCRIPT;
import static dev.paoding.longan.channel.http.Http.ContentType.IMAGE_PNG;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

public abstract class AbstractServiceHandler {
    private final Logger logger = LoggerFactory.getLogger(AbstractServiceHandler.class);
    private final boolean zeroCopyEnabled;
    private final ResponseFilter responseFilter = new ResponseFilter();

    {
        zeroCopyEnabled = !System.getProperty("os.name").toLowerCase().contains("win");
    }

    protected void writeJson(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, HttpResponseStatus httpResponseStatus, ExceptionResult exceptionResult) {
        String content = JsonUtils.toJson(exceptionResult);
        writeJson(ctx, fullHttpRequest, httpResponseStatus, content);
    }

    protected void writeJson(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, String json) {
        write(ctx, fullHttpRequest, HttpResponseStatus.OK, json.getBytes(StandardCharsets.UTF_8), APPLICATION_JSON);
    }

    protected void writeJson(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, String json, Collection<HttpCookie> httpCookies) {
        write(ctx, fullHttpRequest, json.getBytes(StandardCharsets.UTF_8), APPLICATION_JSON, httpCookies);
    }

    protected void writeJson(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, HttpResponseStatus httpResponseStatus, String json) {
        write(ctx, fullHttpRequest, httpResponseStatus, json.getBytes(StandardCharsets.UTF_8), APPLICATION_JSON);
    }

    protected void writeText(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, HttpResponseStatus httpResponseStatus, String text) {
        write(ctx, fullHttpRequest, httpResponseStatus, text.getBytes(StandardCharsets.UTF_8), TEXT_PLAIN);
    }

    protected void writeXml(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, HttpResponseStatus status, String text) {
        write(ctx, fullHttpRequest, status, text.getBytes(StandardCharsets.UTF_8), APPLICATION_XML);
    }

    protected void writeHtml(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, HttpResponseStatus status, String text) {
        write(ctx, fullHttpRequest, status, text.getBytes(StandardCharsets.UTF_8), TEXT_HTML);
    }

    protected void write(Method method, ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, Result result) {
        Object content = result.getValue();
        if (content == null) {
            writeNoContent(ctx, fullHttpRequest);
        } else {
            AsciiString contentType = result.getType();
            if (contentType.equals(APPLICATION_JSON)) {
                if (content instanceof String) {
                    writeJson(ctx, fullHttpRequest, content.toString());
                } else if (content instanceof HttpCookieProvider httpCookieProvider) {
                    String json = JsonUtils.toJson(responseFilter.filter(method, content));
                    writeJson(ctx, fullHttpRequest, json, httpCookieProvider.httpCookies());
                } else {
                    String json = JsonUtils.toJson(responseFilter.filter(method, content));
                    writeJson(ctx, fullHttpRequest, json);
                }
            } else {
                if (content instanceof VirtualFile virtualFile) {
                    write(ctx, fullHttpRequest, virtualFile, contentType);
                } else {
                    write(ctx, fullHttpRequest, content.toString(), contentType);
                }
            }
        }
    }

    protected void writeNoContent(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
        boolean keepAlive = HttpUtil.isKeepAlive(fullHttpRequest);
        HttpVersion httpVersion = fullHttpRequest.protocolVersion();

        DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.NO_CONTENT, Unpooled.wrappedBuffer(new byte[]{}));
        HttpUtil.setContentLength(fullHttpResponse, 0);
        HttpResponse httpResponse = new HttpResponseImpl(fullHttpResponse);
        postHandle(httpResponse);
        writeAndFlush(ctx, keepAlive, httpResponse);
    }

    protected void write(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, String text, AsciiString contentType) {
        write(ctx, fullHttpRequest, HttpResponseStatus.OK, text.getBytes(StandardCharsets.UTF_8), contentType);
    }

    protected void write(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, HttpResponseStatus httpResponseStatus, byte[] bytes, AsciiString contentType) {
        boolean keepAlive = HttpUtil.isKeepAlive(fullHttpRequest);
        HttpVersion httpVersion = fullHttpRequest.protocolVersion();

        DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion,
                httpResponseStatus, Unpooled.wrappedBuffer(bytes));
        fullHttpResponse.headers().set(CONTENT_TYPE, contentType);
        HttpUtil.setContentLength(fullHttpResponse, bytes.length);
        HttpResponse httpResponse = new HttpResponseImpl(fullHttpResponse);
        postHandle(httpResponse);
        writeAndFlush(ctx, keepAlive, httpResponse);
    }

    protected void write(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, byte[] bytes, AsciiString contentType, Collection<HttpCookie> httpCookies) {
        boolean keepAlive = HttpUtil.isKeepAlive(fullHttpRequest);
        HttpVersion httpVersion = fullHttpRequest.protocolVersion();

        DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion,
                HttpResponseStatus.OK, Unpooled.wrappedBuffer(bytes));
        fullHttpResponse.headers().set(CONTENT_TYPE, contentType);
        HttpUtil.setContentLength(fullHttpResponse, bytes.length);

        for (HttpCookie httpCookie : httpCookies) {
            DefaultCookie cookie = new DefaultCookie(httpCookie.name(), httpCookie.value());
            cookie.setPath(httpCookie.path());
            cookie.setMaxAge(httpCookie.maxAge());
            cookie.setDomain(httpCookie.domain());
            cookie.setHttpOnly(httpCookie.isHttpOnly());
            cookie.setSecure(httpCookie.isSecure());
            cookie.setWrap(httpCookie.wrap());
            if (httpCookie.sameSite() != null) {
                cookie.setSameSite(CookieHeaderNames.SameSite.valueOf(httpCookie.sameSite().name()));
            }
            cookie.setPartitioned(httpCookie.isPartitioned());
            ServerCookieEncoder encoder = ServerCookieEncoder.STRICT;
            fullHttpResponse.headers().add(HttpHeaderNames.SET_COOKIE, encoder.encode(cookie));
        }

        HttpResponse httpResponse = new HttpResponseImpl(fullHttpResponse);
        postHandle(httpResponse);
        writeAndFlush(ctx, keepAlive, httpResponse);
    }

    protected void write(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, VirtualFile virtualFile, AsciiString contentType) {
        boolean keepAlive = HttpUtil.isKeepAlive(fullHttpRequest);
        HttpVersion httpVersion = fullHttpRequest.protocolVersion();

        DefaultHttpResponse defaultHttpResponse = new DefaultHttpResponse(httpVersion, HttpResponseStatus.OK);
        String filename = URLEncoder.encode(virtualFile.getName(), StandardCharsets.UTF_8);
        defaultHttpResponse.headers().set(CONTENT_DISPOSITION, "attachment;filename*=UTF-8''" + filename);
        defaultHttpResponse.headers().set(CONTENT_TYPE, contentType);
        defaultHttpResponse.headers().set(CONTENT_LENGTH, virtualFile.length());
        HttpResponse httpResponse = new HttpResponseImpl(defaultHttpResponse, virtualFile);
        postHandle(httpResponse);
        writeAndFlush(ctx, keepAlive, httpResponse);
    }


    protected static AsciiString getContentType(String uri) {
        AsciiString contentType;
        if (uri.endsWith("html") || uri.endsWith("htm")) {
            contentType = TEXT_HTML;
        } else if (uri.endsWith("js")) {
            contentType = APPLICATION_JAVASCRIPT;
        } else if (uri.endsWith("css")) {
            contentType = TEXT_CSS;
        } else if (uri.endsWith("png")) {
            contentType = IMAGE_PNG;
        } else {
            contentType = APPLICATION_OCTET_STREAM;
        }
        return contentType;
    }

    protected void postHandle(HttpResponse httpResponse) {

    }

    protected void writeAndFlush(ChannelHandlerContext ctx, boolean keepAlive, HttpResponse httpResponse) {
        HttpUtil.setKeepAlive(httpResponse.getDefaultHttpResponse(), keepAlive);
        ChannelFuture channelFuture = ctx.writeAndFlush(httpResponse.getDefaultHttpResponse());

        VirtualFile file = httpResponse.getFile();
        if (file == null) {
            if (!keepAlive) {
                channelFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            if (file instanceof BinaryFile binaryFile) {
                try {
                    DownloadListener downloadListener = binaryFile.getDownloadListener();
                    RandomAccessFile randomAccessFile = new RandomAccessFile(binaryFile.getFile(), "r");
                    try {
                        ChannelFuture sendFileFuture;
                        if (zeroCopyEnabled) {
                            sendFileFuture = ctx.write(new DefaultFileRegion(randomAccessFile.getChannel(), 0, file.length()), ctx.newProgressivePromise());
                        } else {
                            sendFileFuture = ctx.write(new HttpChunkedInput(new ChunkedFile(randomAccessFile, 0, file.length(), 8192)), ctx.newProgressivePromise());
                        }
                        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                            @Override
                            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {

                            }

                            @Override
                            public void operationComplete(ChannelProgressiveFuture future) {
                                try {
                                    randomAccessFile.close();
                                } catch (IOException e) {
                                    logger.info(e.getMessage());
                                }
                                if (downloadListener != null) {
                                    downloadListener.onSuccess();
                                }
                            }
                        });
                    } catch (Exception e) {
                        randomAccessFile.close();
                        if (downloadListener != null) {
                            downloadListener.onFailure();
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                ByteFile byteFile = (ByteFile) file;
                ByteBuf byteBuf = Unpooled.wrappedBuffer(byteFile.getBytes());
                ByteBufInputStream contentStream = new ByteBufInputStream(byteBuf);
                ctx.writeAndFlush(new HttpChunkedInput(new ChunkedStream(contentStream)));
            }

            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!keepAlive) {
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}
