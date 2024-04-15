package dev.paoding.longan.data.jpa;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;

public class ParamMap extends HashMap<String, Object> {
    @Override
    public Object put(String key, Object value) {
        if (value instanceof Instant instant) {
            value = Timestamp.from(instant);
        }
        return super.put(key, value);
    }
}
