package dev.paoding.longan.channel.http;


import java.io.File;

public class BinaryFile implements VirtualFile {
    private final String name;
    private final File file;
    private DownloadListener downloadListener;

    public BinaryFile(File file, String name) {
        this.file = file;
        this.name = name;
    }

    public BinaryFile(File file, String name, DownloadListener downloadListener) {
        this.file = file;
        this.name = name;
        this.downloadListener = downloadListener;
    }

    @Override
    public long length() {
        return this.file.length();
    }

    @Override
    public String getName() {
        return name;
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    public File getFile() {
        return file;
    }

}
