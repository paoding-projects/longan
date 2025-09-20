package dev.paoding.longan.channel.http;

public class DefaultHandlerInterceptor implements HandlerInterceptor {
    private final ScopedResult scopedResult = ScopedResult.of(true);

    @Override
    public ScopedResult preHandle(HttpRequest request) {
        return scopedResult;
    }
}
