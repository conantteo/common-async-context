# common-async-context

Shared Spring Boot 3.5.x / Java 17 library for propagating application request context across `@Async` and `CompletableFuture` boundaries.

The library provides:

- `ThreadLocalAccessors`, a small factory for adapting existing `ThreadLocal` holder classes from other libraries.
- An explicit registrar that registers `ThreadLocalAccessor` beans with Micrometer's `ContextRegistry`.
- A `ContextPropagatingTaskDecorator` bean.
- Integration with Spring Boot's default `applicationTaskExecutor`.
- `ContextAwareCompletableFutures`, a small facade that prevents accidental use of the JDK common pool.

## Dependency

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>common-async-context</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

This library is built against Spring Boot `3.5.0` and Java `17`.

## Usage

Expose a `ThreadLocalAccessor` bean for each existing `ThreadLocal` holder that should cross async boundaries:

```java
@Configuration
class UserContextPropagationConfiguration {

    @Bean
    ThreadLocalAccessor<UserContext> userContextThreadLocalAccessor() {
        return ThreadLocalAccessors.create(
                "common-interceptor.user-context",
                UserContext::get,
                UserContext::set,
                UserContext::clear
        );
    }
}
```

Use the actual method names from your context holder. The important contract is:

- `getter`: read the current thread's value when async work is submitted.
- `setter`: install the captured value on the worker thread.
- `resetter`: clear the worker thread after the task completes.

Once the `ThreadLocalAccessor` bean exists, the library auto-registers it with Micrometer's `ContextRegistry`. The configured `ContextPropagatingTaskDecorator` will then capture that context when async work is submitted and restore it on the worker thread.

If `UserContext` is mutable, prefer capturing an immutable snapshot or defensive copy:

```java
@Bean
ThreadLocalAccessor<UserContextSnapshot> userContextThreadLocalAccessor() {
    return ThreadLocalAccessors.create(
            "common-interceptor.user-context",
            () -> UserContextSnapshot.from(UserContext.get()),
            snapshot -> UserContext.set(snapshot.toUserContext()),
            UserContext::clear
    );
}
```

Use `@Async` with Spring Boot's default executor:

```java
@Async("applicationTaskExecutor")
public CompletableFuture<String> loadAsync() {
    UserContext context = UserContext.get();
    return CompletableFuture.completedFuture(context.getTenantId());
}
```

Use `ContextAwareCompletableFutures` instead of raw `CompletableFuture.supplyAsync(...)`:

```java
@Service
class PricingService {

    private final ContextAwareCompletableFutures futures;

    PricingService(ContextAwareCompletableFutures futures) {
        this.futures = futures;
    }

    CompletableFuture<Price> price() {
        return futures.supplyAsync(this::loadPrice);
    }
}
```

Avoid:

```java
CompletableFuture.supplyAsync(this::loadPrice);
```

That uses the JDK common pool and bypasses the Spring `TaskDecorator`.

## Configuration

This library does not create or size its own executor. Configure Spring Boot's default task executor using standard Spring Boot properties:

```yaml
spring:
  task:
    execution:
      thread-name-prefix: app-async-
      pool:
        core-size: 20
        max-size: 100
        queue-capacity: 500
```

Spring Boot creates the `applicationTaskExecutor`. This library contributes the `ContextPropagatingTaskDecorator` early enough for Boot to apply it to that executor.

`ContextAwareCompletableFutures` is wired to the same `applicationTaskExecutor`, so this:

```java
futures.supplyAsync(this::loadPrice);
```

uses the Boot-managed, context-propagating executor.

If an application defines its own `applicationTaskExecutor`, it must apply the `TaskDecorator` to that executor. Spring Boot can only auto-apply the decorator to executors it creates.

## OpenTelemetry

This library does not depend on the OpenTelemetry API and does not read or write trace/span IDs.

With Kubernetes Java auto-instrumentation, the OpenTelemetry Java agent is responsible for trace context propagation through supported async boundaries. This library standardizes your custom `ThreadLocal` propagation and keeps `CompletableFuture` usage on a context-aware executor.
