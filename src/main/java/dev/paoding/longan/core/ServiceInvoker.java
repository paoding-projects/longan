package dev.paoding.longan.core;

import com.google.common.base.Throwables;
import dev.paoding.longan.channel.http.HttpRequestException;
import dev.paoding.longan.channel.http.VirtualFile;
import dev.paoding.longan.data.DataNotFoundException;
import dev.paoding.longan.service.DuplicateException;
import dev.paoding.longan.service.InternalServerException;
import dev.paoding.longan.service.ServiceException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLIntegrityConstraintViolationException;

public abstract class ServiceInvoker extends ResponseFilter {

    protected Result invoke(MethodInvocation methodInvocation, Object[] arguments) {
        try {
            Object value = invoke(methodInvocation.getMethod(), methodInvocation.getService(), arguments);
            Result result = new Result();
            result.setValue(value);
            result.setType(methodInvocation.getResponseType());
            return result;
        } catch (ServiceException e) {
            e.setMethodInvocation(methodInvocation);
            throw e;
        } catch (HttpRequestException e) {
            e.setResponseType(methodInvocation.getResponseType());
            throw e;
        } catch (InternalServerException e) {
            e.setMethodInvocation(methodInvocation);
            throw e;
        }
    }

    protected Object invoke(Method method, Object object, Object[] arguments) {
        Object value;
        try {
            value = method.invoke(object, arguments);
        } catch (InvocationTargetException | IllegalAccessException e) {
            Throwable throwable = Throwables.getRootCause(e);
            Class<?> clazz = throwable.getClass();
            if (ServiceException.class.isAssignableFrom(clazz)) {
                throw (ServiceException) throwable;
            } else if (HttpRequestException.class.isAssignableFrom(clazz)) {
                throw (HttpRequestException) throwable;
            } else if (clazz == EmptyResultDataAccessException.class) {
                throw new DataNotFoundException("data not found");
            } else if (clazz == SQLIntegrityConstraintViolationException.class) {
                throw new DuplicateException("duplicate entry", throwable);
            } else {
                InternalServerException internalServerException = new InternalServerException(throwable.getMessage());
                internalServerException.setStackTrace(throwable.getStackTrace());
                throw internalServerException;
            }
        }
        if (value == null) {
            return null;
        } else if (VirtualFile.class.isAssignableFrom(value.getClass())) {
            return value;
//        } else if (HttpFile.class.isAssignableFrom(value.getClass())) {
//            return value;
        }

        return filter(method, value);
    }


}
