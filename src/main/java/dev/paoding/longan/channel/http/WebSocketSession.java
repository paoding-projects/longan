package dev.paoding.longan.channel.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketSession {
    private static final Pattern pattern = Pattern.compile("^/ws/([^/?]+)");
    private final Map<CharSequence, Object> map = new ConcurrentHashMap<>();
    private final Channel channel;
    private final String requestUri;
    private String anchor;

    public WebSocketSession(Channel channel, String requestUri) {
        this.channel = channel;
        this.requestUri = requestUri;
        Matcher matcher = pattern.matcher(requestUri);
        if (matcher.find()) {
            anchor = matcher.group(1);
        } else {
            channel.close();
        }
    }

    public void close(){
        channel.close();
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

    public String getRequestUri() {
        return this.requestUri;
    }

    public Object put(CharSequence key, Object value) {
        return map.put(key, value);
    }

    public Object get(CharSequence key) {
        return map.get(key);
    }

    public Object remove(CharSequence key) {
        return map.remove(key);
    }

    public void write(String message) {
        if (channel != null && channel.isActive()) {
            TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(message);
            channel.writeAndFlush(textWebSocketFrame);
        }
    }

    public void write(byte[] bytes) {
        if (channel != null && channel.isActive()) {
            BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes));
            channel.writeAndFlush(binaryWebSocketFrame);
        }
    }
}
