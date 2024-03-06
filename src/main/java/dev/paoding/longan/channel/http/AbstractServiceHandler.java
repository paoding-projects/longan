package dev.paoding.longan.channel.http;

import dev.paoding.longan.util.GsonUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static dev.paoding.longan.channel.http.Http.ContentType.APPLICATION_JAVASCRIPT;
import static dev.paoding.longan.channel.http.Http.ContentType.IMAGE_PNG;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

public abstract class AbstractServiceHandler {

    public abstract HttpResponse channelRead(ChannelHandlerContext ctx, FullHttpRequest request);

    protected HttpResponse writeJson(HttpVersion httpVersion, HttpResponseStatus httpResponseStatus, ExceptionResult exceptionResult) {
        String content = GsonUtils.toJson(exceptionResult);
        return writeJson(httpVersion, httpResponseStatus, content);
    }

    protected HttpResponse writeJson(HttpVersion httpVersion, HttpResponseStatus httpResponseStatus, String json) {
        return write(httpVersion, httpResponseStatus, json.getBytes(StandardCharsets.UTF_8), APPLICATION_JSON);
    }

    protected HttpResponse writeText(HttpVersion httpVersion, HttpResponseStatus httpResponseStatus, String text) {
        return write(httpVersion, httpResponseStatus, text.getBytes(StandardCharsets.UTF_8), TEXT_PLAIN);
    }

    protected HttpResponse writeXml(HttpVersion httpVersion, HttpResponseStatus status, String text) {
        return write(httpVersion, status, text.getBytes(StandardCharsets.UTF_8), APPLICATION_XML);
    }

    protected HttpResponse writeHtml(HttpVersion httpVersion, HttpResponseStatus status, String text) {
        return write(httpVersion, status, text.getBytes(StandardCharsets.UTF_8), TEXT_HTML);
    }

    protected HttpResponse writeNoContent(HttpVersion httpVersion) {
        DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.NO_CONTENT, Unpooled.wrappedBuffer(new byte[]{}));
        HttpUtil.setContentLength(fullHttpResponse, 0);
        HttpResponse httpResponse = new HttpResponseImpl(fullHttpResponse);
        postHandle(httpResponse);
        return httpResponse;
    }

    protected HttpResponse write(HttpVersion httpVersion, HttpResponseStatus status, String text, AsciiString contentType) {
        return write(httpVersion, status, text.getBytes(StandardCharsets.UTF_8), contentType);
    }

    protected HttpResponse write(HttpVersion httpVersion, HttpResponseStatus httpResponseStatus, byte[] bytes, AsciiString contentType) {
        DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion,
                httpResponseStatus, Unpooled.wrappedBuffer(bytes));
        fullHttpResponse.headers().set(CONTENT_TYPE, contentType);
        HttpUtil.setContentLength(fullHttpResponse, bytes.length);
        HttpResponse httpResponse = new HttpResponseImpl(fullHttpResponse);
        postHandle(httpResponse);
        return httpResponse;
    }

    protected HttpResponse write(HttpVersion httpVersion, VirtualFile virtualFile, AsciiString contentType) {
        DefaultHttpResponse defaultHttpResponse = new DefaultHttpResponse(httpVersion, HttpResponseStatus.OK);
        String filename = URLEncoder.encode(virtualFile.getName(), StandardCharsets.UTF_8);
        defaultHttpResponse.headers().set(CONTENT_DISPOSITION, "attachment;filename*=UTF-8''" + filename);
        defaultHttpResponse.headers().set(CONTENT_TYPE, contentType);
        defaultHttpResponse.headers().set(CONTENT_LENGTH, virtualFile.length());
        defaultHttpResponse.headers().set(TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
        HttpResponse httpResponse = new HttpResponseImpl(defaultHttpResponse, virtualFile);
        postHandle(httpResponse);
        return httpResponse;
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
}
