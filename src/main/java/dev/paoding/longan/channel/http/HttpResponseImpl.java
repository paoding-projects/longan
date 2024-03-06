package dev.paoding.longan.channel.http;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

public class HttpResponseImpl implements HttpResponse {
    private DefaultHttpResponse response;
    private VirtualFile file;


    public HttpResponseImpl(DefaultHttpResponse response) {
        this.response = response;
    }

    public HttpResponseImpl(DefaultHttpResponse response,VirtualFile file) {
        this.response = response;
        this.file = file;
    }

    @Override
    public VirtualFile getFile() {
        return this.file;
    }

    public DefaultHttpResponse getDefaultHttpResponse() {
        return this.response;
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }

    @Override
    public String headers(String name) {
        return response.headers().get(name);
    }

}
