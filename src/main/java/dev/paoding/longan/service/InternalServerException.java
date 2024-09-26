package dev.paoding.longan.service;

import dev.paoding.longan.core.MethodInvocation;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;


@Getter
public class InternalServerException extends RuntimeException {
    private final String code;
    @Setter
    protected String responseType;
    @Setter
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
}
