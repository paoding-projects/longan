package dev.paoding.longan.channel.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;

public abstract class HttpRequestException extends RuntimeException {
    protected String code;
    protected String responseType;

    public HttpRequestException(String message) {
        super(message);
    }

    public abstract HttpResponseStatus getHttpResponseStatus();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
}
