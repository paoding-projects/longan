package dev.paoding.longan.channel.http;

public interface HandlerInterceptor {

    ScopedResult preHandle(HttpRequest request);

    default void postHandle(HttpResponse response) {

    }

    default void afterCompletion() {

    }
}
