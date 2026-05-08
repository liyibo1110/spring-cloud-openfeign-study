package com.github.liyibo1110.openfeign;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * @author liyibo
 * @date 2026-05-07 10:48
 */
class FeignCircuitBreakerDisabledConditions extends AnyNestedCondition {

    FeignCircuitBreakerDisabledConditions() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnMissingClass("org.springframework.cloud.client.circuitbreaker.CircuitBreaker")
    static class CircuitBreakerClassMissing {

    }

    @ConditionalOnProperty(value = "spring.cloud.openfeign.circuitbreaker.enabled", havingValue = "false", matchIfMissing = true)
    static class CircuitBreakerDisabled {

    }
}
