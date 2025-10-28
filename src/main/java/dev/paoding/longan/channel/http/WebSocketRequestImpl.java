package dev.paoding.longan.channel.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.AsciiString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WebSocketRequestImpl implements HttpRequest {
    public static final AsciiString COOKIE_NAME = AsciiString.cached("Cookie");
    private final HttpHeaders httpHeaders;
    private final String requestUri;
    private Map<String, String> cookieMap;

    public WebSocketRequestImpl(String requestUri, HttpHeaders httpHeaders) {
        this.requestUri = requestUri;
        this.httpHeaders = httpHeaders;
    }

    @Override
    public Map<String, String> getCookies() {
        if (cookieMap != null) {
            return cookieMap;
        }

        cookieMap = new HashMap<>();
        String cookieString = httpHeaders.get(COOKIE_NAME);
        if (cookieString != null) {
            Set<Cookie> cookieSet = ServerCookieDecoder.STRICT.decode(cookieString);
            for (Cookie cookie : cookieSet) {
                cookieMap.put(cookie.name(), cookie.value());
            }
        }

        return cookieMap;
    }

    @Override
    public String getCookie(String name) {
        return getCookies().get(name);
    }

    @Override
    public String getHeader(String name) {
        return httpHeaders.get(name);
    }

    @Override
    public String getHeader(CharSequence name) {
        return httpHeaders.get(name);
    }

    @Override
    public String getHeader(CharSequence name, String defaultValue) {
        return httpHeaders.get(name, defaultValue);
    }

    @Override
    public List<String> getHeaders(String name) {
        return httpHeaders.getAll(name);
    }

    @Override
    public List<String> getHeaders(CharSequence name) {
        return httpHeaders.getAll(name);
    }

    @Override
    public String getMethod() {
        throw new UnsupportedOperationException("getMethod() not implemented");
    }

    @Override
    public String getUri() {
        return requestUri;
    }

    @Override
    public String getPath() {
        throw new UnsupportedOperationException("getPath() not implemented");
    }

    @Override
    public ByteBuf getContent() {
        throw new UnsupportedOperationException("getContent() not implemented");
    }
}
