package com.example.asynccontext;

import io.micrometer.context.ThreadLocalAccessor;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ThreadLocalAccessors {

    private ThreadLocalAccessors() {
    }

    public static <T> ThreadLocalAccessor<T> create(
            String key,
            Supplier<T> getter,
            Consumer<T> setter,
            Runnable resetter) {

        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        Objects.requireNonNull(resetter, "resetter must not be null");

        return new ThreadLocalAccessor<>() {
            @Override
            public Object key() {
                return key;
            }

            @Override
            public T getValue() {
                return getter.get();
            }

            @Override
            public void setValue(T value) {
                setter.accept(value);
            }

            @Override
            public void reset() {
                resetter.run();
            }
        };
    }
}
