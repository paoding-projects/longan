package dev.paoding.longan.service;

import dev.paoding.longan.channel.http.HttpRequestException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class MismatchException extends HttpRequestException {
    public MismatchException(String name) {
        super(name + " do not match");
        this.code = name + ".mismatch";
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return HttpResponseStatus.BAD_REQUEST;
    }
}
