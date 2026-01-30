package dev.paoding.longan.channel.http;

import dev.paoding.longan.annotation.RequestBody;
import dev.paoding.longan.annotation.RequestHeader;
import dev.paoding.longan.annotation.RequestParam;
import dev.paoding.longan.core.MethodInvocation;
import dev.paoding.longan.core.Result;
import dev.paoding.longan.core.ServiceInvoker;
import dev.paoding.longan.data.Between;
import dev.paoding.longan.service.MethodNotAllowedException;
import dev.paoding.longan.service.SystemException;
import dev.paoding.longan.service.UnsupportedMediaTypeException;
import dev.paoding.longan.util.JsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;


@Component
public class HttpServiceInvoker extends ServiceInvoker {
    private final AntPathMatcher matcher = new AntPathMatcher();
    private static final String APPLICATION_JSON = "application/json";
    private static final int APPLICATION_JSON_LEN = APPLICATION_JSON.length();

    public Result invokeService(MethodInvocation methodInvocation, String path, String query, FullHttpRequest httpRequest) throws SystemException {
        HttpMethod httpMethod = httpRequest.method();
        HttpHeaders headers = httpRequest.headers();
        HttpDataEntity httpDataEntity = parseQueryParameter(methodInvocation.getPath(), path, query);

        Object[] arguments;
        if (httpMethod == HttpMethod.POST) {
            String contentType = headers.get(HttpHeaderNames.CONTENT_TYPE);
            if (contentType == null) {
                throw new UnsupportedMediaTypeException(methodInvocation.getResponseType());
            }
            if (isApplicationJson(contentType)) {
                String json = httpRequest.content().toString(CharsetUtil.UTF_8);
                arguments = parseJsonArguments(methodInvocation, headers, httpDataEntity, json);
                return invoke(methodInvocation, arguments);
            } else if (HttpPostRequestDecoder.isMultipart(httpRequest)) {
                HttpDataFactory factory = new DefaultHttpDataFactory(true);
                HttpPostMultipartRequestDecoder decoder = new HttpPostMultipartRequestDecoder(factory, httpRequest);
                try {
                    parseMultipartFormData(decoder, httpDataEntity);
                    arguments = parseArguments(methodInvocation, headers, httpDataEntity);
                    return invoke(methodInvocation, arguments);
                } finally {
                    decoder.destroy();
                }
            } else if (contentType.equals(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())) {
                parseFormUrlEncoded(httpRequest, httpDataEntity);
                arguments = parseArguments(methodInvocation, headers, httpDataEntity);
                return invoke(methodInvocation, arguments);
            } else {
                arguments = parseOtherArguments(methodInvocation, httpRequest.headers(), httpDataEntity, httpRequest.content());
                return invoke(methodInvocation, arguments);
            }
        } else if (httpMethod == HttpMethod.GET) {
            arguments = parseArguments(methodInvocation, headers, httpDataEntity);
            return invoke(methodInvocation, arguments);
        } else {
            throw new MethodNotAllowedException(methodInvocation.getResponseType());
        }
    }

    private boolean isApplicationJson(String contentType) {
        return contentType.regionMatches(
                true,
                0,
                APPLICATION_JSON,
                0,
                APPLICATION_JSON_LEN
        );
    }

    private Object[] parseJsonArguments(MethodInvocation methodInvocation, HttpHeaders headers, HttpDataEntity httpDataEntity, String json) {
        Parameter[] parameters = methodInvocation.getParameters();
        Object[] arguments = new Object[parameters.length];
        if (methodInvocation.isAnnotationPresent()) {
            if (methodInvocation.hasRequestBody()) {
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    if (parameter.isAnnotationPresent(RequestParam.class)) {
                        arguments[i] = methodInvocation.getRequestParamParameter(httpDataEntity, parameter);
                    } else if (parameter.isAnnotationPresent(RequestHeader.class)) {
                        arguments[i] = getHeader(headers, parameter);
                    } else if (parameter.isAnnotationPresent(RequestBody.class)) {
//                        arguments[i] = JsonUtils.fromJson(json, parameter.getParameterizedType());
                        arguments[i] = json;
                    }
                    methodInvocation.validateParameter(i, arguments[i]);
                }
            } else {
                Map<String, JsonNode> jsonNodeMap = JsonUtils.toMap(json);
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    if (parameter.isAnnotationPresent(RequestParam.class)) {
                        arguments[i] = methodInvocation.getRequestParamParameter(httpDataEntity, parameter);
                    } else if (parameter.isAnnotationPresent(RequestHeader.class)) {
                        arguments[i] = getHeader(headers, parameter);
                    } else {
                        arguments[i] = getJsonNodeMap(jsonNodeMap, parameter);
                    }
                    methodInvocation.validateParameter(i, arguments[i]);
                }
            }
        } else {
            Map<String, JsonNode> jsonNodeMap = JsonUtils.toMap(json);
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                arguments[i] = getJsonNodeMap(jsonNodeMap, parameter);
                methodInvocation.validateParameter(i, arguments[i]);
            }
        }
        return arguments;
    }

    private Object getHeader(HttpHeaders headers, Parameter parameter) {
        RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
        Class<?> parameterType = parameter.getType();
        if (String.class == parameterType) {
            return headers.get(requestHeader.value());
        } else if (List.class == parameterType) {
            return headers.getAll(requestHeader.value());
        }
        return null;
    }

    private Object getJsonNodeMap(Map<String, JsonNode> jsonNodeMap, Parameter parameter) {
        JsonNode jsonNode = jsonNodeMap.get(parameter.getName());
        Object object = null;
        if (jsonNode != null) {
            object = JsonUtils.fromJson(jsonNode, parameter.getParameterizedType());
            if (Between.class.isAssignableFrom(parameter.getType())) {
                Between<?> between = (Between<?>) object;
                between.setField(parameter.getName());
            }
        }
        return object;
    }

    private Object[] parseOtherArguments(MethodInvocation methodInvocation, HttpHeaders headers, HttpDataEntity httpDataEntity, ByteBuf body) {
        Parameter[] parameters = methodInvocation.getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(RequestParam.class)) {
                arguments[i] = methodInvocation.getRequestParamParameter(httpDataEntity, parameter);
            } else if (parameter.isAnnotationPresent(RequestHeader.class)) {
                arguments[i] = getHeader(headers, parameter);
            } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                arguments[i] = methodInvocation.getRequestBodyParameter(body, parameter);
            }
            methodInvocation.validateParameter(i, arguments[i]);
        }
        return arguments;
    }

    private Object[] parseArguments(MethodInvocation methodInvocation, HttpHeaders headers, HttpDataEntity httpDataEntity) {
        Parameter[] parameters = methodInvocation.getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(RequestParam.class)) {
                arguments[i] = methodInvocation.getRequestParamParameter(httpDataEntity, parameter);
            } else if (parameter.isAnnotationPresent(RequestHeader.class)) {
                arguments[i] = getHeader(headers, parameter);
            } else {
                arguments[i] = methodInvocation.getParameter(httpDataEntity, parameter);
            }
            methodInvocation.validateParameter(i, arguments[i]);
        }
        return arguments;
    }

    private HttpDataEntity parseQueryParameter(String mapping, String path, String query) {
        HttpDataEntity httpDataEntity = new HttpDataEntity(query);
        if (matcher.isPattern(mapping)) {
            httpDataEntity.putAll(matcher.extractUriTemplateVariables(mapping, path));
        }
        return httpDataEntity;
    }

    private void parseMultipartFormData(HttpPostMultipartRequestDecoder requestDecoder, HttpDataEntity httpDataEntity) {
        try {
            List<InterfaceHttpData> httpDataList = requestDecoder.getBodyHttpDatas();
            for (InterfaceHttpData data : httpDataList) {
                if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    FileUpload fileUpload = (FileUpload) data;
                    if (fileUpload.isCompleted()) {
                        httpDataEntity.put(fileUpload.getName(), new MultipartFile(fileUpload));
                    }
                } else if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    Attribute attribute = (Attribute) data;
                    httpDataEntity.put(attribute.getName(), attribute.getValue());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseFormUrlEncoded(FullHttpRequest httpRequest, HttpDataEntity httpDataEntity) {
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(httpRequest);
        try {
            List<InterfaceHttpData> httpDataList = decoder.getBodyHttpDatas();
            for (InterfaceHttpData data : httpDataList) {
                if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    Attribute attribute = (Attribute) data;
                    httpDataEntity.put(attribute.getName(), attribute.getValue());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            decoder.destroy();
        }
    }

}
