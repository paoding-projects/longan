package dev.paoding.longan.channel.http;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketListenerHandler {
    private final Map<String, WebSocketListener> webSocketListenerMap = new ConcurrentHashMap<>();
    private final WebSocketListener webSocketListener;

    {
        webSocketListener = new WebSocketListener() {
        };
    }

    public void addWebSocketListener(String anchor, WebSocketListener webSocketListener) {
        webSocketListenerMap.put(anchor, webSocketListener);
    }

    public void onOpen(WebSocketContext context, HttpRequest httpRequest) {
        getOrDefault(context).onOpen(context, httpRequest);
    }

    public void onMessage(WebSocketContext context, String message) {
        getOrDefault(context).onMessage(context, message);
    }

    public void onMessage(WebSocketContext context, byte[] bytes) {
        getOrDefault(context).onMessage(context, bytes);
    }

    public void onClose(WebSocketContext context) {
        getOrDefault(context).onClose(context);
    }

    private WebSocketListener getOrDefault(WebSocketContext context) {
        String anchor = context.getAnchor();
        return webSocketListenerMap.getOrDefault(anchor, webSocketListener);
    }
}
