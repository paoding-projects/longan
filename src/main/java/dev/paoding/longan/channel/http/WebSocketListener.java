package dev.paoding.longan.channel.http;

public interface WebSocketListener {

    default boolean onOpen(WebSocketSession session) {
        return true;
    }

    default boolean onMessage(WebSocketSession session, String message) {
        return true;
    }

    default boolean onMessage(WebSocketSession session, byte[] bytes) {
        return true;
    }

    default boolean onClose(WebSocketSession session) {
        return true;
    }
}
