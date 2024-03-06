package dev.paoding.longan.channel.http;

import lombok.Getter;

import java.io.File;

@Getter
public class HttpFile implements VirtualFile {
    private final String name;
    private final File file;
    private DownloadListener downloadListener;

    public HttpFile(File file, String name) {
        this.file = file;
        this.name = name;
    }

    public HttpFile(File file, String name, DownloadListener downloadListener) {
        this.file = file;
        this.name = name;
        this.downloadListener = downloadListener;
    }

    public long length() {
        return this.file.length();
    }

}
