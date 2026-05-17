package com.example.asynccontext.autoconfigure;

import com.example.asynccontext.ContextAwareCompletableFutures;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;

class CommonAsyncContextAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CommonAsyncContextAutoConfiguration.class,
                    TaskExecutionAutoConfiguration.class,
                    ContextAwareCompletableFuturesAutoConfiguration.class));

    @Test
    void contributesContextPropagatingTaskDecorator() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(TaskDecorator.class));
    }

    @Test
    void backsOffWhenApplicationDefinesTaskDecorator() {
        contextRunner.withUserConfiguration(CustomTaskDecoratorConfiguration.class)
                .run(context -> assertThat(context.getBean(TaskDecorator.class))
                        .isSameAs(context.getBean("customTaskDecorator")));
    }

    @Test
    void createsContextAwareCompletableFuturesWhenBootApplicationTaskExecutorExists() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("applicationTaskExecutor");
            assertThat(context).hasSingleBean(ContextAwareCompletableFutures.class);
            assertThat(context.getBean(ContextAwareCompletableFutures.class).executor())
                    .isSameAs(context.getBean("applicationTaskExecutor"));
        });
    }

    @Test
    void backsOffWhenApplicationDefinesContextAwareCompletableFutures() {
        contextRunner.withUserConfiguration(CustomCompletableFuturesConfiguration.class)
                .run(context -> assertThat(context.getBean(ContextAwareCompletableFutures.class))
                        .isSameAs(context.getBean("customContextAwareCompletableFutures")));
    }

    @Test
    void doesNotCreateContextAwareCompletableFuturesWithoutApplicationTaskExecutor() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ContextAwareCompletableFuturesAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(ContextAwareCompletableFutures.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTaskDecoratorConfiguration {

        @Bean
        TaskDecorator customTaskDecorator() {
            return runnable -> runnable;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomCompletableFuturesConfiguration {

        @Bean
        ContextAwareCompletableFutures customContextAwareCompletableFutures() {
            return new ContextAwareCompletableFutures(Runnable::run);
        }
    }
}
