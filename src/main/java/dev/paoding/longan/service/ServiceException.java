package dev.paoding.longan.service;

import dev.paoding.longan.core.MethodInvocation;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ServiceException extends RuntimeException {
    protected String code;
    @Setter
    protected String responseType;
    @Setter
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

}
