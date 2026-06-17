package dev.paoding.longan.channel.http;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SSEListenerHandler {
    private final Map<String, SSEListener> sseListenerMap = new ConcurrentHashMap<>();
    private final SSEListener sseListener;

    {
        sseListener = new SSEListener() {
        };
    }

    public void addSSEListener(String anchor, SSEListener sseListener) {
        sseListenerMap.put(anchor, sseListener);
    }

    public void onOpen(SSEContext context, HttpRequest httpRequest) {
        getOrDefault(context).onOpen(context, httpRequest);
    }

    public void onClose(SSEContext context) {
        getOrDefault(context).onClose(context);
    }

    private SSEListener getOrDefault(SSEContext context) {
        String anchor = context.getAnchor();
        return sseListenerMap.getOrDefault(anchor, sseListener);
    }
}
