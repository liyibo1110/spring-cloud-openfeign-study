package com.github.liyibo1110.openfeign;

import feign.Feign;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

/**
 * Feign接口与CircuitBreaker组件配合使用。
 * @author liyibo
 * @date 2026-05-08 13:00
 */
public final class FeignCircuitBreaker {

    private FeignCircuitBreaker() {
        throw new IllegalStateException("Don't instantiate a utility class");
    }

    public static final class Builder extends Feign.Builder {
        private CircuitBreakerFactory circuitBreakerFactory;

        private String feignClientName;

        private boolean circuitBreakerGroupEnabled;

        private CircuitBreakerNameResolver circuitBreakerNameResolver;

        Builder circuitBreakerFactory(CircuitBreakerFactory circuitBreakerFactory) {
            this.circuitBreakerFactory = circuitBreakerFactory;
            return this;
        }

        Builder feignClientName(String feignClientName) {
            this.feignClientName = feignClientName;
            return this;
        }

        Builder circuitBreakerGroupEnabled(boolean circuitBreakerGroupEnabled) {
            this.circuitBreakerGroupEnabled = circuitBreakerGroupEnabled;
            return this;
        }

        Builder circuitBreakerNameResolver(CircuitBreakerNameResolver circuitBreakerNameResolver) {
            this.circuitBreakerNameResolver = circuitBreakerNameResolver;
            return this;
        }

        public Feign build(final FallbackFactory<?> nullableFallbackFactory) {
            super.invocationHandlerFactory((target, dispatch) -> new FeignCircuitBreakerInvocationHandler(
                    circuitBreakerFactory, feignClientName, target, dispatch, nullableFallbackFactory,
                    circuitBreakerGroupEnabled, circuitBreakerNameResolver));
            return super.build();
        }
    }
}
