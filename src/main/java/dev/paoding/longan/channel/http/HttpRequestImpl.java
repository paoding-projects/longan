package dev.paoding.longan.channel.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpRequestImpl implements HttpRequest {
    private final FullHttpRequest request;
    private String path;
    private Map<String, String> cookieMap;

    public HttpRequestImpl(FullHttpRequest request, String path) {
        this.request = request;
        this.path = path;
    }

    @Override
    public Map<String, String> getCookies() {
        if (cookieMap != null) {
            return cookieMap;
        }

        cookieMap = new HashMap<>();
        String cookieString = request.headers().get("Cookie");
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
        return request.headers().get(name);
    }

    @Override
    public String getHeader(CharSequence name) {
        return request.headers().get(name);
    }

    @Override
    public String getHeader(CharSequence name, String defaultValue) {
        return request.headers().get(name, defaultValue);
    }

    @Override
    public List<String> getHeaders(String name) {
        return request.headers().getAll(name);
    }

    @Override
    public List<String> getHeaders(CharSequence name) {
        return request.headers().getAll(name);
    }

    @Override
    public String getMethod() {
        return request.method().name();
    }

    @Override
    public String getUri() {
        return request.uri();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public ByteBuf getContent() {
        return request.content();
    }
}
