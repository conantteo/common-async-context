package com.example.asynccontext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class ContextAwareCompletableFutures {

    private final Executor executor;

    public ContextAwareCompletableFutures(Executor executor) {
        this.executor = executor;
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executor);
    }

    public Executor executor() {
        return executor;
    }
}
