package dev.paoding.longan.channel.http;

import lombok.Getter;

import java.io.File;

@Getter
public class HttpFile implements VirtualFile {
    private final String name;
    private final File file;
    private FileListener fileListener;

    public HttpFile(File file, String name) {
        this.file = file;
        this.name = name;
    }

    public HttpFile(File file, String name, FileListener fileListener) {
        this.file = file;
        this.name = name;
        this.fileListener = fileListener;
    }

    public long length() {
        return this.file.length();
    }

}
