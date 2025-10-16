package dev.paoding.longan.channel.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.ReferenceCountUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Component
public class HttpHandler {
    private final Logger logger = LoggerFactory.getLogger(HttpHandler.class);
    @Resource
    private ApiServiceHandler apiServiceHandler;
    @Resource
    private DocServiceHandler docServiceHandler;
    @Resource
    private OptionsServiceHandler optionsServiceHandler;
    @Resource
    private NotFoundServiceHandler notFoundServiceHandler;
    private static final String API_PREFIX = "/api/";
    private static final String DOC_PREFIX = "/doc/";
    private final ExecutorService executorService;

    {
        ThreadFactory threadFactory = Thread.ofVirtual().name("http-thread-", 0).uncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                logger.error(thread.getName(), throwable);
            }
        }).factory();
        executorService = Executors.newThreadPerTaskExecutor(threadFactory);
    }

    public void channelRead(ChannelHandlerContext ctx, FullHttpRequest request) {
        executorService.execute(() -> {
            try {
                if (request.method() == HttpMethod.OPTIONS) {
                    optionsServiceHandler.channelRead(ctx, request);
                } else {
                    String uri = request.uri();
                    if (uri.startsWith(API_PREFIX)) {
                        apiServiceHandler.channelRead(ctx, request, uri.substring(4));
                    } else if (uri.startsWith(DOC_PREFIX)) {
                        docServiceHandler.channelRead(ctx, request, uri);
                    } else {
                        notFoundServiceHandler.channelRead(ctx, request);
                    }
                }
            } finally {
                ReferenceCountUtil.release(request);
            }
        });
    }
}
