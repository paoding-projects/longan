package dev.paoding.longan.channel.http;

public interface WebSocketListener {

    default void onOpen(WebSocketContext context,HttpRequest httpRequest) {
    }

    default void onMessage(WebSocketContext context, String message) {
    }

    default void onMessage(WebSocketContext context, byte[] bytes) {
    }

    default void onClose(WebSocketContext context) {
    }
}
