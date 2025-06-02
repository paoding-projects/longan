package dev.paoding.longan.channel.http;

public interface WebSocketListener {

    default void onOpen(WebSocketSession session) {
    }

    default void onMessage(WebSocketSession session, String message) {
    }

    default void onMessage(WebSocketSession session, byte[] bytes) {
    }

    default void onClose(WebSocketSession session) {
    }
}
