package dev.paoding.longan.service;

import dev.paoding.longan.core.MethodInvocation;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;


public class InternalServerException extends RuntimeException {
    private final String code;
    protected String responseType;
    protected MethodInvocation methodInvocation;

    public InternalServerException(Throwable cause) {
        super(cause);
        this.code = "internal.server.error";
    }

    public InternalServerException(String message) {
        super(message);
        this.code = "internal.server.error";
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
        this.code = "internal.server.error";
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }

    public String getCode() {
        return code;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public MethodInvocation getMethodInvocation() {
        return methodInvocation;
    }

    public void setMethodInvocation(MethodInvocation methodInvocation) {
        this.methodInvocation = methodInvocation;
    }
}
