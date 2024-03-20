package dev.paoding.longan.channel.http;

import io.netty.handler.codec.http.multipart.FileUpload;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.*;

public class MultipartFile {
    public static final Set<PosixFilePermission> permissions = EnumSet.of(OWNER_READ, OWNER_WRITE, GROUP_READ);
    public static final boolean supportsPosix;
    private final FileUpload fileUpload;

    static {
        FileStore fileStore = FileSystems.getDefault().getFileStores().iterator().next();
        supportsPosix = fileStore.supportsFileAttributeView(PosixFileAttributeView.class);
    }

    public MultipartFile(FileUpload fileUpload) {
        this.fileUpload = fileUpload;
    }

    public String getContentType() {
        return fileUpload.getContentType();
    }

    public long length() {
        return fileUpload.length();
    }

    public String getFilename() {
        return fileUpload.getFilename();
    }

    public String getName() {
        return this.fileUpload.getName();
    }

    public boolean transferTo(File file) throws IOException {
        boolean result = fileUpload.renameTo(file);
        if (result && supportsPosix) {
            Files.setPosixFilePermissions(file.toPath(), permissions);
        }
        return result;
    }

    public File getFile() throws IOException {
        return fileUpload.getFile();
    }

    @Override
    public String toString() {
        return getFilename();
    }
}
