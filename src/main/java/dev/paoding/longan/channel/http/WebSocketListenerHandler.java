package dev.paoding.longan.channel.http;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public void onOpen(WebSocketSession session) {
        getOrDefault(session).onOpen(session);
    }

    public void onMessage(WebSocketSession session, String message) {
        getOrDefault(session).onMessage(session, message);
    }

    public void onMessage(WebSocketSession session, byte[] bytes) {
        getOrDefault(session).onMessage(session, bytes);
    }

    public void onClose(WebSocketSession session) {
        getOrDefault(session).onClose(session);
    }

    private WebSocketListener getOrDefault(WebSocketSession session) {
        String anchor = session.getAnchor();
        return webSocketListenerMap.getOrDefault(anchor, webSocketListener);
    }
}
