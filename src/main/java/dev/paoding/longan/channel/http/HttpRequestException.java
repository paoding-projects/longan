package dev.paoding.longan.channel.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class HttpRequestException extends RuntimeException {
    protected String code;
    @Setter
    protected String responseType;

    public HttpRequestException(String message) {
        super(message);
    }


    public abstract HttpResponseStatus getHttpResponseStatus();


}
