package dev.paoding.longan.channel.http;

import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;

public class FileReadWriteFutureListener implements ChannelProgressiveFutureListener {
    private final Logger logger = LoggerFactory.getLogger(FileReadWriteFutureListener.class);
    private final RandomAccessFile randomAccessFile;
    private final DownloadListener downloadListener;

    public FileReadWriteFutureListener(RandomAccessFile randomAccessFile, DownloadListener downloadListener) {
        this.randomAccessFile = randomAccessFile;
        this.downloadListener = downloadListener;
    }

    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {

    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) {
        try {
            randomAccessFile.close();
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
        if (downloadListener != null) {
            if (future.isSuccess()) {
                downloadListener.onSuccess();
            } else {
                downloadListener.onFailure();
            }
        }
    }
}
