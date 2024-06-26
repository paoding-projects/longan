package dev.paoding.longan.service;

import dev.paoding.longan.channel.http.HttpRequestException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class PasswordMismatchException extends HttpRequestException {
    public PasswordMismatchException() {
        super("Password do not match");
        this.code = "password.mismatch";
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return HttpResponseStatus.BAD_REQUEST;
    }
}
