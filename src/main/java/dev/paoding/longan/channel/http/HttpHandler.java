package dev.paoding.longan.channel.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;

@Slf4j
@Component
public class HttpHandler {
    @Resource
    private HttpServiceHandler httpServiceHandler;
    @Resource
    private DocServiceHandler docServiceHandler;
    @Value("${longan.http.cross-origin:false}")
    private Boolean enableCrossOrigin;
    private static final String API_PREFIX = "/api/";
    private static final String DOC_PREFIX = "/doc/";
    private final ExecutorService executorService;
    private final boolean zeroCopyEnabled;

    {
        ThreadFactory threadFactory = Thread.ofVirtual().name("http-thread-", 0).uncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                log.error(throwable.getMessage());
            }
        }).factory();
        executorService = Executors.newThreadPerTaskExecutor(threadFactory);

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            zeroCopyEnabled = false;
        } else {
            zeroCopyEnabled = true;
        }
    }

    public void channelRead(ChannelHandlerContext ctx, FullHttpRequest request) {
        executorService.execute(() -> {
            try {
                HttpResponse httpResponse;
                if (request.method() == HttpMethod.OPTIONS) {
                    DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.NO_CONTENT);
                    if (enableCrossOrigin) {
                        HttpHeaders httpHeaders = fullHttpResponse.headers();
                        httpHeaders.set("Access-Control-Allow-Origin", "*");
                        httpHeaders.set("Access-Control-Allow-Methods", "*");
                        httpHeaders.set("Access-Control-Allow-Headers", "*");
                        httpHeaders.set("Access-Control-Allow-Credentials", "true");
                    }
                    httpResponse = new HttpResponseImpl(fullHttpResponse);
                } else {
                    String uri = request.uri();
                    if (uri.startsWith(API_PREFIX)) {
                        httpResponse = httpServiceHandler.channelRead(ctx, request);
                    } else if (uri.startsWith(DOC_PREFIX)) {
                        httpResponse = docServiceHandler.channelRead(ctx, request);
                    } else {
                        String message = "Not found " + uri;
                        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
                        DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.NOT_FOUND, Unpooled.wrappedBuffer(bytes));
                        fullHttpResponse.headers().set(CONTENT_TYPE, TEXT_PLAIN);
                        HttpUtil.setContentLength(fullHttpResponse, bytes.length);
                        httpResponse = new HttpResponseImpl(fullHttpResponse);
                    }
                }
                boolean keepAlive = HttpUtil.isKeepAlive(request);
                HttpUtil.setKeepAlive(httpResponse.getDefaultHttpResponse(), keepAlive);
                ChannelFuture channelFuture = ctx.writeAndFlush(httpResponse.getDefaultHttpResponse());

                VirtualFile file = httpResponse.getFile();
                if (file == null) {
                    if (!keepAlive) {
                        channelFuture.addListener(ChannelFutureListener.CLOSE);
                    }
                } else {
                    if (file instanceof HttpFile httpFile) {
                        DownloadListener downloadListener = httpFile.getDownloadListener();
                        RandomAccessFile randomAccessFile = new RandomAccessFile(httpFile.getFile(), "r");
                        try {
                            ChannelFuture sendFileFuture;
                            if (zeroCopyEnabled) {
                                sendFileFuture = ctx.write(new DefaultFileRegion(randomAccessFile.getChannel(), 0, file.length()), ctx.newProgressivePromise());
                            } else {
                                sendFileFuture = ctx.write(new HttpChunkedInput(new ChunkedFile(randomAccessFile, 0, file.length(), 8192)), ctx.newProgressivePromise());
                            }
                            sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                                @Override
                                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {

                                }

                                @Override
                                public void operationComplete(ChannelProgressiveFuture future) {
                                    try {
                                        randomAccessFile.close();
                                    } catch (IOException e) {
                                        log.info(e.getMessage());
                                    }
                                    if (downloadListener != null) {
                                        downloadListener.onSuccess();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            randomAccessFile.close();
                            if (downloadListener != null) {
                                downloadListener.onFailure();
                            }
                        }
                    } else {
                        ByteFile byteFile = (ByteFile) file;
                        ByteBuf byteBuf = Unpooled.wrappedBuffer(byteFile.getBytes());
                        ByteBufInputStream contentStream = new ByteBufInputStream(byteBuf);
                        ctx.writeAndFlush(new HttpChunkedInput(new ChunkedStream(contentStream)));
                    }

                    ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                    if (!keepAlive) {
                        lastContentFuture.addListener(ChannelFutureListener.CLOSE);
                    }
                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                ReferenceCountUtil.release(request);
            }
        });
    }
}
