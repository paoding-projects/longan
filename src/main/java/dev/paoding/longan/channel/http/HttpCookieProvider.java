package dev.paoding.longan.channel.http;

import java.util.Collection;

public interface HttpCookieProvider {
    Collection<HttpCookie> cookies();

    void addHttpCookie(HttpCookie httpCookie);
}
