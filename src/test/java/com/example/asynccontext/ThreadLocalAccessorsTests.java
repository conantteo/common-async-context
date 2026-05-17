package com.example.asynccontext;

import io.micrometer.context.ThreadLocalAccessor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ThreadLocalAccessorsTests {

    @Test
    void delegatesThreadLocalAccessorOperations() {
        AtomicReference<String> value = new AtomicReference<>("tenant-a");

        ThreadLocalAccessor<String> accessor = ThreadLocalAccessors.create(
                "test.key",
                value::get,
                value::set,
                () -> value.set(null)
        );

        assertThat(accessor.key()).isEqualTo("test.key");
        assertThat(accessor.getValue()).isEqualTo("tenant-a");

        accessor.setValue("tenant-b");
        assertThat(value).hasValue("tenant-b");

        accessor.reset();
        assertThat(value.get()).isNull();
    }

    @Test
    void requiresKey() {
        assertThatNullPointerException()
                .isThrownBy(() -> ThreadLocalAccessors.create(null, () -> null, value -> {
                }, () -> {
                }))
                .withMessage("key must not be null");
    }

    @Test
    void requiresGetter() {
        assertThatNullPointerException()
                .isThrownBy(() -> ThreadLocalAccessors.create("test.key", null, value -> {
                }, () -> {
                }))
                .withMessage("getter must not be null");
    }

    @Test
    void requiresSetter() {
        assertThatNullPointerException()
                .isThrownBy(() -> ThreadLocalAccessors.create("test.key", () -> null, null, () -> {
                }))
                .withMessage("setter must not be null");
    }

    @Test
    void requiresResetter() {
        assertThatNullPointerException()
                .isThrownBy(() -> ThreadLocalAccessors.create("test.key", () -> null, value -> {
                }, null))
                .withMessage("resetter must not be null");
    }
}
