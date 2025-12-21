package dev.paoding.longan.channel.http;

import java.util.Collection;

public interface HttpCookieProvider {
    Collection<HttpCookie> httpCookies();

    void addHttpCookie(HttpCookie httpCookie);
}
