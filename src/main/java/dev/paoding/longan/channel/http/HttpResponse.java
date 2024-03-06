package dev.paoding.longan.channel.http;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;


public interface HttpResponse {

    VirtualFile getFile();

    DefaultHttpResponse getDefaultHttpResponse();

    HttpHeaders headers();

    String headers(String name);

}
