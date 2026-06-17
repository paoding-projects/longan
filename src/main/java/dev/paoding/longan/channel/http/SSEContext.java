package dev.paoding.longan.channel.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SSEContext {
    private static final ByteBuf PING_BUF = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(": ping\n\n".getBytes(StandardCharsets.UTF_8)));
    private static final ByteBuf RETRY_BUF = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("retry: 0\n\n".getBytes(StandardCharsets.UTF_8)));
    private static final Pattern pattern = Pattern.compile("^/([^/]+)");
    private final Map<String, Object> map = new ConcurrentHashMap<>();
    private ScheduledFuture<?> scheduledFuture;
    private final Channel channel;
    private String anchor;

    public SSEContext(Channel channel, String requestUri) {
        this.channel = channel;
        Matcher matcher = pattern.matcher(requestUri);
        if (matcher.find()) {
            anchor = matcher.group(1);
            scheduledFuture = channel.eventLoop().scheduleAtFixedRate(() -> {
                if (!channel.isActive()) {
                    cancelSchedule();
                    return;
                }
                channel.writeAndFlush(PING_BUF.duplicate()).addListener((future -> {
                    if (!future.isSuccess()) {
                        cancelSchedule();
                        channel.close();
                    }
                }));
            }, 0, 3, TimeUnit.MINUTES);
        } else {
            channel.close();
        }
    }

    protected void cancelSchedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    public void close() {
        cancelSchedule();
        channel.write(RETRY_BUF.duplicate());
        channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }

    protected void destroy() {
        cancelSchedule();
        channel.close();
        map.clear();
    }

    protected String getAnchor() {
        return this.anchor;
    }

    public String getId() {
        if (channel != null) {
            return channel.id().asLongText();
        }

        return null;
    }

    public Object put(String key, Object value) {
        return map.put(key, value);
    }

    public Object get(String key) {
        return map.get(key);
    }

    public Object remove(String key) {
        return map.remove(key);
    }

    public void write(SSEEvent event) {
        if (channel.isActive()) {
            ByteBuf buf = Unpooled.wrappedBuffer(event.toBytes());
            channel.writeAndFlush(buf).addListener(future -> {
                if (!future.isSuccess()) {
                    ReferenceCountUtil.safeRelease(buf);
                }
            });
        }
    }

}
