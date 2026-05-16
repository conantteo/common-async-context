package com.example.asynccontext.autoconfigure;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.context.ThreadLocalAccessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;

import java.util.Set;
import java.util.stream.Collectors;

@AutoConfiguration(before = TaskExecutionAutoConfiguration.class)
@ConditionalOnClass({ContextSnapshotFactory.class, ContextPropagatingTaskDecorator.class})
public class CommonAsyncContextAutoConfiguration {

    public static final String APPLICATION_TASK_EXECUTOR_BEAN_NAME = "applicationTaskExecutor";

    @Bean
    @ConditionalOnMissingBean(name = "commonAsyncContextRegistryRegistrar")
    SmartInitializingSingleton commonAsyncContextRegistryRegistrar(ObjectProvider<ThreadLocalAccessor<?>> accessors) {
        return () -> {
            ContextRegistry registry = ContextRegistry.getInstance();
            Set<Object> registeredKeys = registry.getThreadLocalAccessors().stream()
                    .map(ThreadLocalAccessor::key)
                    .collect(Collectors.toSet());

            accessors.orderedStream()
                    .filter(accessor -> !registeredKeys.contains(accessor.key()))
                    .forEach(registry::registerThreadLocalAccessor);
        };
    }

    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    TaskDecorator contextPropagatingTaskDecorator(ObjectProvider<ContextSnapshotFactory> snapshotFactory) {
        ContextSnapshotFactory factory = snapshotFactory.getIfAvailable();
        return factory != null ? new ContextPropagatingTaskDecorator(factory) : new ContextPropagatingTaskDecorator();
    }
}
