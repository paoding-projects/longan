package dev.paoding.longan.data.json;

import dev.paoding.longan.util.DateTimeUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalTime;

public class LocalTimeDeserializer extends ValueDeserializer<LocalTime> {
    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
        return DateTimeUtils.parseTime(p.getString());
    }
}
