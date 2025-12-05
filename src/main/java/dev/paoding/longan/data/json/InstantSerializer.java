package dev.paoding.longan.data.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.Instant;

public class InstantSerializer extends ValueSerializer<Instant> {

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializationContext ctx) throws JacksonException {
        gen.writeNumber(value.toEpochMilli());
    }
}
