package dev.paoding.longan.channel.http;

import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Map;

public interface HttpRequest {
    String getHeader(String name);

    String getHeader(CharSequence name);

    String getHeader(CharSequence name, String defaultValue);

    List<String> getHeaders(String name);

    List<String> getHeaders(CharSequence name);

    Map<String, String> getCookies();

    String getCookie(String name);

    String getMethod();

    String getUri();

    String getPath();

    ByteBuf getContent();
}
