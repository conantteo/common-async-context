package com.example.asynccontext;

import com.example.asynccontext.autoconfigure.CommonAsyncContextAutoConfiguration;
import com.example.asynccontext.autoconfigure.ContextAwareCompletableFuturesAutoConfiguration;
import io.micrometer.context.ThreadLocalAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ContextAwareCompletableFuturesTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CommonAsyncContextAutoConfiguration.class,
                    TaskExecutionAutoConfiguration.class,
                    ContextAwareCompletableFuturesAutoConfiguration.class))
            .withUserConfiguration(TestUserContextConfiguration.class);

    @AfterEach
    void clearContext() {
        TestUserContext.clear();
    }

    @Test
    void propagatesRegisteredThreadLocalContextToCompletableFuture() {
        contextRunner.run(context -> {
            TestUserContext.set("tenant-a");

            ContextAwareCompletableFutures futures = context.getBean(ContextAwareCompletableFutures.class);

            String result = futures.supplyAsync(TestUserContext::get)
                    .get(5, TimeUnit.SECONDS);

            assertThat(result).isEqualTo("tenant-a");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestUserContextConfiguration {

        @Bean
        ThreadLocalAccessor<String> testUserContextThreadLocalAccessor() {
            return ThreadLocalAccessors.create(
                    "test.user-context",
                    TestUserContext::get,
                    TestUserContext::set,
                    TestUserContext::clear
            );
        }
    }

    static final class TestUserContext {

        private static final ThreadLocal<String> VALUE = new ThreadLocal<>();

        private TestUserContext() {
        }

        static String get() {
            return VALUE.get();
        }

        static void set(String value) {
            VALUE.set(value);
        }

        static void clear() {
            VALUE.remove();
        }
    }
}
