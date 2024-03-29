package dev.paoding.longan.core;

import dev.paoding.longan.annotation.*;
import dev.paoding.longan.service.ConstraintViolationException;
import dev.paoding.longan.service.UnexpectedJsonDataException;
import dev.paoding.longan.service.UnsupportedParameterTypeException;
import dev.paoding.longan.channel.http.HttpDataEntity;
import dev.paoding.longan.channel.http.RequestParameterException;
import dev.paoding.longan.util.GsonUtils;
import dev.paoding.longan.validation.BeanCleaner;
import dev.paoding.longan.validation.ParameterValidator;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MethodInvocation extends ParameterValidator {
    private final BeanCleaner beanCleaner = new BeanCleaner();
    private final Map<String, Validator> validatorMap = new HashMap<>();
    private final Map<String, Param> paramMap = new HashMap<>();
    private Class<?> serviceInterface;
    private Class<?> serviceClass;
    private Object service;
    private Method method;
    private String path;
    private int lineNumber;
    private String responseType;
    private boolean hasRequestBody;
    private Parameter[] parameters;

    public MethodInvocation(Class<?> serviceClass, MethodDescriptor methodDescriptor, String path) {
        this.path = path;
        this.serviceClass = serviceClass;
        this.method = methodDescriptor.getMethod();
        this.lineNumber = methodDescriptor.getLineNumber();
        setMethod(methodDescriptor.getMethod());
    }

    public MethodInvocation(Class<?> serviceInterface, Class<?> serviceClass, MethodDescriptor methodDescriptor) {
        this.serviceInterface = serviceInterface;
        this.serviceClass = serviceClass;
        this.method = methodDescriptor.getMethod();
        this.lineNumber = methodDescriptor.getLineNumber();
        setMethod(methodDescriptor.getMethod());
    }

//    public void setInterface(Class<?> interfaceClass) {
//        this.interfaceClass = interfaceClass;
//    }

    public Class<?> getServiceInterface() {
        return this.serviceInterface;
    }

    public Class<?> getServiceClass() {
        return this.serviceClass;
    }

    public Object getParameter(HttpDataEntity httpDataEntity, Parameter parameter) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        String name;
        if (requestParam != null) {
            name = requestParam.value();
        } else {
            name = parameter.getName();
        }
        try {
            return httpDataEntity.getValue(parameter, name);
        } catch (NumberFormatException e) {
            throw new RequestParameterException(e.getMessage());
        }
    }


    public Object getParameter(boolean isApplicationJson, ByteBuf body, Parameter parameter) {
        Object object = null;
        if (body.readableBytes() > 0) {
            Class<?> parameterType = parameter.getType();
            if (parameterType == String.class) {
                object = body.toString(StandardCharsets.UTF_8);
            } else if (parameterType.isArray() && parameterType.getComponentType() == byte.class) {
                byte[] bytes = new byte[body.readableBytes()];
                body.readBytes(bytes);
                object = bytes;
            } else if (isApplicationJson) {
                try {
                    object = GsonUtils.fromJson(body.toString(StandardCharsets.UTF_8), parameter.getParameterizedType());
                    cleanParameter(parameter, object, paramMap.get(parameter.getName()), validatorMap);
                } catch (Exception e) {
                    throw new UnexpectedJsonDataException(parameter.getName());
                }
            } else {
                throw new UnsupportedParameterTypeException(parameter.getName());
            }
        }
        body.clear();
        return object;
    }

    public void validateParameter(int index, Object argument) {
        Parameter parameter = parameters[index];
        if (paramMap.containsKey(parameter.getName())) {
            Param param = paramMap.get(parameter.getName());
            try {
                validateParameter(parameter, argument, param, validatorMap);
            } catch (ConstraintViolationException e) {
                e.setResponseType(this.responseType);
                throw e;
            }
            beanCleaner.cleanParameter(parameter, argument, param, validatorMap);
        } else {
            //todo 没有验证参数
        }
    }

    public Map<String, Validator> getValidatorMap() {
        return validatorMap;
    }

    public Map<String, Param> getParamMap() {
        return paramMap;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public Object getService() {
        return service;
    }

    public void setService(Object service) {
        this.service = service;
    }

    public Method getMethod() {
        return method;
    }

    public boolean hasRequestBody() {
        return this.hasRequestBody;
    }

    private void setMethod(Method method) {
        this.method = method;
        this.parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                hasRequestBody = true;
                break;
            }
        }

        if (method.isAnnotationPresent(Mapping.class)) {
            Mapping mapping = method.getAnnotation(Mapping.class);
            this.responseType = mapping.responseType();
        }

        if (method.isAnnotationPresent(Request.class)) {
            Request request = method.getAnnotation(Request.class);
            Validator[] validators = request.validators();
            for (Validator validator : validators) {
                validatorMap.put(validator.type().getName() + validator.id(), validator);
            }

            Param[] params = request.params();
            for (Param param : params) {
                paramMap.put(param.name(), param);
            }
        }
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
