package dev.paoding.longan.data.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.paoding.longan.data.jpa.BeanProxy;
import tools.jackson.core.Version;
import tools.jackson.databind.*;
import tools.jackson.databind.ser.Serializers;

public class LonganModule extends JacksonModule {

    @Override
    public String getModuleName() {
        return "LonganModule";
    }

    @Override
    public Version version() {
        return new Version(1, 0, 0, null, "dev.paoding", "longan");
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new Serializers.Base() {
            @Override
            public ValueSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription.Supplier beanDescRef, JsonFormat.Value formatOverrides) {
                if (BeanProxy.class.isAssignableFrom(type.getRawClass())) {
                    return new BeanProxySerializer();
                }
                if (Class.class.isAssignableFrom(type.getRawClass())) {
                    return new ClassSerializer();
                }
                return null;
            }
        });
    }
}
