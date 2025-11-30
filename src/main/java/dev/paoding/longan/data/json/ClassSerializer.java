package dev.paoding.longan.data.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClassSerializer extends ValueSerializer<Class<?>> {

    @Override
    public void serialize(Class<?> value, JsonGenerator gen, SerializationContext ctx) throws JacksonException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(value.getName());
        }
    }
}
