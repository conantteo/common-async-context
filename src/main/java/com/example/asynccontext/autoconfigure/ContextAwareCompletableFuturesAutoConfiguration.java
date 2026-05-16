package com.example.asynccontext.autoconfigure;

import com.example.asynccontext.ContextAwareCompletableFutures;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;

@AutoConfiguration(after = TaskExecutionAutoConfiguration.class)
@ConditionalOnClass(ContextAwareCompletableFutures.class)
public class ContextAwareCompletableFuturesAutoConfiguration {

    @Bean
    @ConditionalOnBean(name = CommonAsyncContextAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    @ConditionalOnMissingBean
    ContextAwareCompletableFutures contextAwareCompletableFutures(
            @Qualifier(CommonAsyncContextAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME) Executor executor) {
        return new ContextAwareCompletableFutures(executor);
    }
}
