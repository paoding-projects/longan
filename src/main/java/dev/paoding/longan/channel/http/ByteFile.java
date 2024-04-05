package dev.paoding.longan.channel.http;


public class ByteFile implements VirtualFile {
    private final byte[] bytes;
    private final String name;

    public ByteFile(byte[] bytes, String name) {
        this.bytes = bytes;
        this.name = name;
    }

    @Override
    public long length() {
        return bytes.length;
    }

    @Override
    public String getName() {
        return name;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
