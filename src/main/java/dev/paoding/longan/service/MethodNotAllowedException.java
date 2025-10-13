package dev.paoding.longan.service;

import io.netty.handler.codec.http.HttpResponseStatus;

public class MethodNotAllowedException extends RuntimeException {
    private static final String code = "method.not.allowed";
    private static final String message = "The method not allowed, allow: GET, POST.";
    private String responseType;

    public MethodNotAllowedException(String responseType) {
        this.responseType = responseType;
    }

    public String getCode() {
        return code;
    }

    public String getResponseType() {
        return responseType;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return HttpResponseStatus.METHOD_NOT_ALLOWED;
    }
}
