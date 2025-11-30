package dev.paoding.longan.data.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.Instant;

public class InstantDeserializer extends ValueDeserializer<Instant> {
    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
        return Instant.ofEpochMilli(p.getLongValue());
    }
}
