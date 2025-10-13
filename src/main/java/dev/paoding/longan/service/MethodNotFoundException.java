package dev.paoding.longan.service;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.Serial;


public class MethodNotFoundException extends RuntimeException  {
    @Serial
    private static final long serialVersionUID = 0L;
    private static final String code = "method.not.found";
    public MethodNotFoundException(String message) {
        super(message);
    }

    public String getCode() {
        return code;
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return HttpResponseStatus.NOT_FOUND;
    }

}
