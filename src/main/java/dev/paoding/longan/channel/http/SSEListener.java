package dev.paoding.longan.channel.http;

public interface SSEListener {

    default void onOpen(SSEContext context, HttpRequest httpRequest) {
    }

    default void onClose(SSEContext context) {
    }
}
