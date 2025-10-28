package dev.paoding.longan.channel.http;

public class DefaultHandlerInterceptor implements HandlerInterceptor {
    private final ScopedContext scopedResult = new ScopedContext();

    @Override
    public ScopedContext preHandle(HttpRequest request) {
        return scopedResult;
    }
}
