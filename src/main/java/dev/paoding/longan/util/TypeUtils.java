package dev.paoding.longan.util;

import org.springframework.data.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TypeUtils {
    private static final Map<String, Field> nameFieldMapCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> typeFieldMapCache = new ConcurrentHashMap<>();

    public static String getLowerSimpleName(Class<?> type) {
        return StringUtils.underline(type.getSimpleName()).toLowerCase();
    }

    public static String getUpperSimpleName(Class<?> type) {
        return StringUtils.underline(type.getSimpleName()).toUpperCase();
    }

    public static List<Field> getDeclaredFields(Class<?> type) {
        if (typeFieldMapCache.containsKey(type)) {
            return typeFieldMapCache.get(type);
        }
        List<Field> fieldList = new ArrayList<>();
        loadFiled(fieldList, type);
        return fieldList;
    }

    public static Collection<Field> getAllDeclaredFields(Class<?> type) {
        Map<String, Field> fieldMap = new LinkedHashMap<>();
        ReflectionUtils.findField(type, field -> {
            String name = field.getName();
            if (fieldMap.containsKey(name)) {
                if (!equals(fieldMap.get(name), field)) {
                    throw new RuntimeException(fieldMap.get(name) + " conflicts with " + field);
                }
            } else {
                fieldMap.put(name, field);
            }

            return false;
        });
        return fieldMap.values();
    }

    private static boolean equals(Field left, Field right) {
        if (left.getType().equals(right.getType())) {
            if (Collection.class.isAssignableFrom(left.getType())) {
                if ((left.getGenericType() instanceof ParameterizedType leftType) && (right.getGenericType() instanceof ParameterizedType rightType)) {
                    return equals(leftType, rightType);
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private static boolean equals(ParameterizedType left, ParameterizedType right) {
        return left.getActualTypeArguments()[0].equals(right.getActualTypeArguments()[0]);
    }


    private static void loadFiled(List<Field> fieldList, Class<?> type) {
//        Field[] fields = type.getDeclaredFields();
        Collection<Field> fields = getAllDeclaredFields(type);
        for (Field field : fields) {
            int modifier = field.getModifiers();
            if (modifier == Modifier.PRIVATE || modifier == Modifier.PUBLIC || modifier == Modifier.PROTECTED) {
                field.setAccessible(true);
                fieldList.add(field);
            }
        }
//        if (type.getSuperclass() != null) {
//            loadFiled(fieldList, type.getSuperclass());
//        }
    }

    public static Field getField(Class<?> type, String filedName) {
        String typeName = type.getTypeName();
        String key = typeName + "." + filedName;
        if (nameFieldMapCache.containsKey(key)) {
            return nameFieldMapCache.get(key);
        }
        loadFiled(typeName, type);
        return nameFieldMapCache.get(key);
    }

    private static void loadFiled(String typeName, Class<?> type) {
//        Field[] fields = type.getDeclaredFields();
        Collection<Field> fields = getAllDeclaredFields(type);
        for (Field field : fields) {
            int modifier = field.getModifiers();
            if (modifier == Modifier.PRIVATE || modifier == Modifier.PUBLIC || modifier == Modifier.PROTECTED) {
                nameFieldMapCache.put(typeName + "." + field.getName(), field);
            }
        }
//        if (type.getSuperclass() != null) {
//            loadFiled(typeName, type.getSuperclass());
//        }
    }

    public static List<String> getEnumValues(Class<?> enumType) {
        return Arrays.stream((Enum<?>[]) enumType.getEnumConstants())
                .map(Enum::name)
                .toList();
//        return EnumSet.allOf(enumType).stream()
//                .map(Enum::name)
//                .collect(Collectors.toList());
    }

}
