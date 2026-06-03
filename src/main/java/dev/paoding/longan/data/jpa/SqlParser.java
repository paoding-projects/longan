package dev.paoding.longan.data.jpa;


import dev.paoding.longan.data.Between;
import dev.paoding.longan.data.Entity;
import dev.paoding.longan.data.Pageable;
import dev.paoding.longan.data.ShortValueEnum;
import dev.paoding.longan.service.ServiceException;
import dev.paoding.longan.util.EntityUtils;
import dev.paoding.longan.util.StringUtils;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.Map;

public class SqlParser {

    public static Map<String, Object> toMap(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        Map<String, Object> paramMap = new ParamMap();
        try {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg != null) {
                    Class<?> type = arg.getClass();
//                    if (type == Pageable.class) {
//                        paramMap.put(parameters[i].getName(), arg);
//                    } else
                    if (type.isAnnotationPresent(Entity.class)) {
                        Field[] declaredFields = arg.getClass().getDeclaredFields();
                        for (Field field : declaredFields) {
                            if (field.getModifiers() == Modifier.PRIVATE) {
                                if (Collection.class.isAssignableFrom(field.getType())) {
                                    Type fieldType = field.getGenericType();
                                    if (fieldType instanceof ParameterizedType) {
                                        Class<?> subType = (Class<?>) ((ParameterizedType) fieldType).getActualTypeArguments()[0];
                                        if (subType.isAnnotationPresent(Entity.class)) {
                                            break;
                                        }
                                    }
                                }
                                field.setAccessible(true);
                                Object value = field.get(arg);
                                if (value != null) {
                                    if (field.getType().isAnnotationPresent(Entity.class)) {
                                        Object id = EntityUtils.getId(value);
                                        if (id != null) {
                                            paramMap.put(parameters[i].getName() + "." + field.getName() + ".id", id);
                                        }
                                    } else {
                                        paramMap.put(parameters[i].getName() + "." + field.getName(), value);
                                    }
                                }
                            }
                        }
                    } else if (Between.class.isAssignableFrom(type)) {
                        Between<?> between = (Between<?>) arg;
                        paramMap.put(parameters[i].getName() + ".start", between.getStart());
                        paramMap.put(parameters[i].getName() + ".end", between.getEnd());
                    } else if (ShortValueEnum.class.isAssignableFrom(type)) {
                        paramMap.put(parameters[i].getName(), ((ShortValueEnum) arg).value());
//                    } else if (type.isArray()) {
//                        paramMap.put(SqlParser.toDatabaseName(parameters[i].getName()), Database.createArrayOf(arg));
                    } else {
//                        paramMap.put(SqlParser.toDatabaseName(parameters[i].getName()), arg);
                        paramMap.put(parameters[i].getName(), arg);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return paramMap;
    }

    public static String getLinkTable(String source, String target, String role) {
        source = toDatabaseName(source);
        target = toDatabaseName(target);
        if (role == null) {
            role = "";
        } else if (!role.isEmpty()) {
            role = "_" + role;
        }
        if (source.compareTo(target) < 0) {
            return source + "_" + target + role;
        } else {
            return target + "_" + source + role;
        }
    }

    public static String toCountSql(String source, String target, String role) {
        if (!role.isEmpty()) {
            role = "_" + role;
        }
        if (source.compareTo(target) < 0) {
            String tableName = source + "_" + target + role;
            return "SELECT COUNT(*) FROM " + tableName + " WHERE " + source + "_id = :" + source + "_id AND " + target + "_id = :" + target + "_id";
        } else {
            String tableName = target + "_" + source + role;
            return "SELECT COUNT(*) FROM " + tableName + " WHERE " + target + "_id = :" + target + "_id AND " + source + "_id = :" + source + "_id";
        }
    }

    public static String toJoinSql(String database, String source, String target, String role) {
        if (!role.isEmpty()) {
            role = "_" + role;
        }
        if (source.compareTo(target) < 0) {
            String tableName = source + "_" + target + role;
            if (database.equals(Database.POSTGRESQL)) {
                return "INSERT INTO " + tableName + " (" + source + "_id, " + target + "_id) VALUES (:" + source + "_id, :" + target + "_id) ON CONFLICT (" + source + "_id, " + target + "_id) DO NOTHING";
            } else if (database.equals(Database.MYSQL)) {
                return "INSERT IGNORE INTO " + tableName + " (" + source + "_id, " + target + "_id) VALUES (:" + source + "_id, :" + target + "_id)";
            }
        } else {
            String tableName = target + "_" + source + role;
            if (database.equals(Database.POSTGRESQL)) {
                return "INSERT INTO " + tableName + " (" + target + "_id, " + source + "_id) VALUES (:" + target + "_id, :" + source + "_id) ON CONFLICT (" + target + "_id, " + source + "_id) DO NOTHING";
            } else if (database.equals(Database.MYSQL)) {
                return "INSERT IGNORE INTO " + tableName + " (" + target + "_id, " + source + "_id) VALUES (:" + target + "_id, :" + source + "_id)";
            }
        }
        throw new ServiceException("Unsupported database type");
    }

    public static String toSplitSql(String source, String target, String role) {
        if (!role.isEmpty()) {
            role = "_" + role;
        }
        if (source.compareTo(target) < 0) {
            String tableName = source + "_" + target + role;
            return "DELETE FROM " + tableName + " WHERE " + source + "_id = :" + source + "_id AND " + target + "_id = :" + target + "_id";
        } else {
            String tableName = target + "_" + source + role;
            return "DELETE FROM " + tableName + " WHERE " + target + "_id = :" + target + "_id AND " + source + "_id = :" + source + "_id";
        }
    }

    public static String toSplitSqlAll(String source, String target, String role) {
        if (!role.isEmpty()) {
            role = "_" + role;
        }
        String tableName;
        if (source.compareTo(target) < 0) {
            tableName = source + "_" + target + role;
        } else {
            tableName = target + "_" + source + role;
        }
        return "DELETE FROM " + tableName + " WHERE " + source + "_id = :" + source + "_id";
    }

    public static String toSplitSqlAllWithoutParameter(String source, String target, String role) {
        if (!role.isEmpty()) {
            role = "_" + role;
        }
        String tableName;
        if (source.compareTo(target) < 0) {
            tableName = source + "_" + target + role;
        } else {
            tableName = target + "_" + source + role;
        }
        return "DELETE FROM " + tableName;
    }

    public static String toColumnName(String name) {
        return StringUtils.lower(name);
    }

    public static String toDatabaseName(String name) {
        int j = name.indexOf("$");
        if (j > 0) {
            name = name.substring(0, j);
        }

        return toColumnName(name);
    }

}
