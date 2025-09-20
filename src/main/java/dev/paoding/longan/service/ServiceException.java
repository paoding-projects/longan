package dev.paoding.longan.service;

import dev.paoding.longan.core.MethodInvocation;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;

public class ServiceException extends RuntimeException {
    protected String code;
    protected String responseType;
    protected MethodInvocation methodInvocation;


    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(Throwable cause) {
        super(cause);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
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

    public MethodInvocation getMethodInvocation() {
        return methodInvocation;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public void setMethodInvocation(MethodInvocation methodInvocation) {
        this.methodInvocation = methodInvocation;
    }
}
