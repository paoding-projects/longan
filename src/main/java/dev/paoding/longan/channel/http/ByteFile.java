package dev.paoding.longan.channel.http;

import lombok.Getter;

@Getter
public class ByteFile implements VirtualFile {
    private final byte[] bytes;
    private final String name;

    public ByteFile(byte[] bytes, String name) {
        this.bytes = bytes;
        this.name = name;
    }

    public long length() {
        return bytes.length;
    }
}
