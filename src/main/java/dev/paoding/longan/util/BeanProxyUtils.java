package dev.paoding.longan.util;

import dev.paoding.longan.data.jpa.BeanProxy;

import java.lang.reflect.Method;

public class BeanProxyUtils {
    public static Method GET_ORIGINAL_METHOD;
    public static Method GET_TYPE_METHOD;
    public final static String GET_ID_METHOD = "getId";

    static {
        for (Method method : BeanProxy.class.getMethods()) {
            if (method.getName().equals("getOriginal")) {
                GET_ORIGINAL_METHOD = method;
            } else if (method.getName().equals("getType")) {
                GET_TYPE_METHOD = method;
            }
        }
    }
}
