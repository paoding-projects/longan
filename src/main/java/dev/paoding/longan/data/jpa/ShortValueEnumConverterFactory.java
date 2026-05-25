package dev.paoding.longan.data.jpa;

import dev.paoding.longan.data.ShortValueEnum;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

public class ShortValueEnumConverterFactory implements ConverterFactory<Short, ShortValueEnum> {

    @Override
    public <T extends ShortValueEnum> Converter<Short, ? extends T> getConverter(@NonNull Class<T> targetType) {
        return new ShortValueEnumConverter<>(targetType);
    }

    private record ShortValueEnumConverter<T extends ShortValueEnum>(Class<T> enumType) implements Converter<Short, T> {

        @Override
        public T convert(@NonNull Short source) {
            for (T constant : enumType.getEnumConstants()) {
                if (constant.value() == source) {
                    return constant;
                }
            }
            return null;
        }
    }
}
