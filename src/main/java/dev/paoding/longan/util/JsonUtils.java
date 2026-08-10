package dev.paoding.longan.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.paoding.longan.data.json.LonganModule;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.Map;

public class JsonUtils {
    private final static JsonMapper mapper = JsonMapper.builder()
            .addModule(new LonganModule())
            .changeDefaultPropertyInclusion(incl ->
                    incl.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    public static String toJson(Object object) {
        return mapper.writeValueAsString(object);
    }

    public static String toPrettyJson(Object object) {
        return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return mapper.readValue(json, clazz);
    }

    public static <T> T fromJson(String json, Type type) {
        JavaType javaType = mapper.constructType(type);
        return mapper.readValue(json, javaType);
    }

    public static Map<String, JsonNode> toMap(String json) {
        return mapper.readValue(json, new TypeReference<>() {
        });
    }

    public static Map<String, String> toSimpleMap(String json) {
        return mapper.readValue(json, new TypeReference<>() {
        });
    }

    public static <T> T fromJson(JsonNode jsonNode, Type type) {
        JavaType javaType = mapper.constructType(type);
        return mapper.convertValue(jsonNode, javaType);
    }
}
