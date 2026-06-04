package dev.paoding.longan.doc;

import dev.paoding.longan.core.ClassPathBeanScanner;
import dev.paoding.longan.data.Pageable;
import dev.paoding.longan.data.jpa.Column;
import dev.paoding.longan.data.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaModelService {
    public static final Map<String, MetaModel> metaModelIndex = new HashMap<>();
    public static final Map<String, MetaField> metaFieldIndex = new HashMap<>();

    public static MetaModel getMetaModel(String id) {
        return metaModelIndex.get(id);
    }

    public static MetaField getMetaField(String id) {
        return metaFieldIndex.get(id);
    }

    public static String getMetaModelAlias(String id) {
        return metaModelIndex.get(id).getAlias();
    }

    public static String getMetaFieldAlias(String id) {
        if (metaFieldIndex.containsKey(id)) {
            return metaFieldIndex.get(id).getAlias();
        }
        return "";
    }

    public static void load() {
//        List<Class<?>> classList = LonganClassScanner.getEntityClassList();
        List<Class<?>> classList = ClassPathBeanScanner.getAllEntityClasses();
        for (Class<?> type : classList) {
            if (type.isAnnotationPresent(Entity.class)) {
//                if(type.getName().endsWith("Entity")) {
//                    loadEntity(type);
//                }
                loadEntity(type);
            }
        }

        loadPageable();
    }


    private static String getAlias(Class<?> clazz) {
        Entity entity = clazz.getAnnotation(Entity.class);
        return entity.alias();
    }

    private static void loadSuperEntity(MetaModel metaModel, Class<?> superClass) {
        if (superClass.getSuperclass() != null) {
            loadSuperEntity(metaModel, superClass.getSuperclass());
        }
        loadFields(metaModel, superClass);
    }

    private static void loadFields(MetaModel metaModel, Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            int modifier = field.getModifiers();
            if (modifier == Modifier.PRIVATE || modifier == Modifier.PUBLIC || modifier == Modifier.PROTECTED) {
                MetaField metaField = new MetaField();
                metaField.setName(field.getName());

                Class<?> type = field.getType();
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType parameterizedType) {
                    metaField.setType((Class<?>) parameterizedType.getRawType());
                    Class<?> actualType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                    metaField.setActualType(actualType);
                    if (actualType.isAnnotationPresent(Entity.class)) {
                        metaField.setActualTypeIsModel(true);
                        metaField.setActualJavaType(actualType.getName());
                        metaField.setAlias(getAlias(actualType));
                    }
                }else if(type.isArray()){
                    metaField.setType(java.sql.Array.class);
                    metaField.setActualType(type.getComponentType());
                } else {
                    metaField.setType(type);
                    if (field.getType().isAnnotationPresent(Entity.class)) {
                        metaField.setTypeIsModel(true);
                        metaField.setAlias(getAlias((Class<?>) genericType));
                    }

                    if (LocalDate.class.isAssignableFrom(type)) {
                        metaField.setDescription("yyyy-MM-dd");
                    } else if (LocalDateTime.class.isAssignableFrom(type)) {
                        metaField.setDescription("yyyy-MM-dd HH:mm:ss");
                    }
                }

                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    metaField.setAlias(column.alias());
                    try {
                        metaField.setSample(column.example());
                    } catch (NumberFormatException e) {
                        throw new DocumentException(new DocumentProblem("Expected " + type + " format, '" + column.example() + "' could not be parsed. ", field));
                    }
                    if(!column.description().isBlank()) {
                        metaField.setDescription(column.description());
                    }
                } else {
                    if (metaField.getAlias() == null) {
                        metaField.setAlias("");
                    }
                    metaField.setSample("");
                }

                if (metaField.getName().equals("id")) {
                    metaField.setAlias("id");
                    if (Number.class.isAssignableFrom(type)) {
                        metaField.setSample(1);
                    } else if (CharSequence.class.isAssignableFrom(type)) {
                        metaField.setSample("A10001");
                    }
                }

                metaModel.addMetaField(metaField);
                metaFieldIndex.put(metaModel.getName() + "." + metaField.getName(), metaField);
            }

        }
    }

    public static void loadEntity(Class<?> clazz) {
        Entity entity = clazz.getAnnotation(Entity.class);
        MetaModel metaModel = new MetaModel();
        metaModel.setName(clazz.getName());
        metaModel.setSimpleName(clazz.getSimpleName());
        metaModel.setAlias(entity.alias());
        metaModel.setDescription(entity.description());


        if (clazz.getSuperclass() != null) {
            loadSuperEntity(metaModel, clazz.getSuperclass());
        }

        loadFields(metaModel, clazz);
        metaModelIndex.put(metaModel.getName(), metaModel);
    }

    public static void loadPageable() {
        Class<?> clazz = Pageable.class;
        MetaModel metaModel = new MetaModel();
        metaModel.setName(clazz.getName());
        metaModel.setSimpleName(clazz.getSimpleName());
        metaModel.setAlias("Pageable");
        metaModel.setDescription("");

        loadFields(metaModel, clazz);
        metaModelIndex.put(metaModel.getName(), metaModel);
    }
}
