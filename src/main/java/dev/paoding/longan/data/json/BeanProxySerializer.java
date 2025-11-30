package dev.paoding.longan.data.json;

import dev.paoding.longan.data.jpa.BeanProxy;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class BeanProxySerializer extends ValueSerializer<BeanProxy> {

    @Override
    public void serialize(BeanProxy value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        Object original = value.getOriginal();
        Class<?> baseType = original.getClass();
        ValueSerializer<Object> delegate = ctxt.findValueSerializer(baseType);
        delegate.serialize(original, gen, ctxt);
    }
}
