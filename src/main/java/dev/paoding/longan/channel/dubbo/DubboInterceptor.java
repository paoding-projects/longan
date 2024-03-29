package dev.paoding.longan.channel.dubbo;

import dev.paoding.longan.annotation.Param;
import dev.paoding.longan.annotation.Validator;
import dev.paoding.longan.core.MethodInvocation;
import dev.paoding.longan.core.ResponseFilter;
import dev.paoding.longan.service.ConstraintViolationException;
import dev.paoding.longan.validation.BeanCleaner;
import dev.paoding.longan.validation.BeanValidator;
import org.apache.dubbo.rpc.RpcException;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DubboInterceptor extends ResponseFilter {
    private final Map<String, List<MethodInvocation>> MethodInvocations = new ConcurrentHashMap<>();
    private final BeanValidator beanValidator = new BeanValidator();
    private final BeanCleaner beanCleaner = new BeanCleaner();

    public void add(MethodInvocation methodInvocation){
       String key = methodInvocation.getServiceInterface().getName() + "." + methodInvocation.getServiceInterface().getName();
        if (!MethodInvocations.containsKey(key)) {
            MethodInvocations.put(key, new ArrayList<>());
        }
        MethodInvocations.get(key).add(methodInvocation);
    }

    public void validate(String serviceName, String methodName, Class<?>[] parameterTypes, Object[] objects) {
        MethodInvocation methodInvocation = get(serviceName + "." + methodName, serviceName, methodName, parameterTypes);
        Map<String, Validator> validatorMap = methodInvocation.getValidatorMap();
        Map<String, Param> paramMap = methodInvocation.getParamMap();
        Parameter[] parameters = methodInvocation.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = objects[i];
            if (paramMap.containsKey(parameter.getName())) {
                Param param = paramMap.get(parameter.getName());
                try {
                    beanValidator.validateParameter(parameter, value, param, validatorMap);
                } catch (ConstraintViolationException e) {
                    throw new RpcException(e);
                }
                beanCleaner.cleanParameter(parameter, value, param, validatorMap);
            }
        }
    }

    public Object filter(String serviceName, String methodName, Class<?>[] parameterTypes, Object value) {
        MethodInvocation methodInvocation = get(serviceName + "." + methodName, serviceName, methodName, parameterTypes);
        return filter(methodInvocation.getMethod(), value);
    }

    public MethodInvocation get(String mapping, String serviceName, String methodName, Class<?>[] parameterTypes) {
        if (MethodInvocations.containsKey(mapping)) {
            List<MethodInvocation> methodInvocationList = MethodInvocations.get(mapping);
            for (MethodInvocation methodInvocation : methodInvocationList) {
                if (equals(methodInvocation.getMethod().getParameterTypes(), parameterTypes)) {
                    return methodInvocation;
                }
            }
        }
        throw new RuntimeException("not found " + serviceName + "." + methodName);
    }

    private boolean equals(Class<?>[] candidateParameterTypes, Class<?>[] requiredParameterTypes) {
        if (candidateParameterTypes.length != requiredParameterTypes.length) {
            return false;
        }
        for (int i = 0; i < candidateParameterTypes.length; i++) {
            if (candidateParameterTypes[i] != requiredParameterTypes[i]) {
                return false;
            }
        }
        return true;
    }
}
