package dev.paoding.longan.channel.http;

public enum ChannelType {
    IO_URING,
    EPOLL,
    KQUEUE,
    NIO;
}
