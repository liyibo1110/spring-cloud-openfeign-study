package com.github.liyibo1110.openfeign;

import feign.Target;

import java.lang.reflect.Method;

/**
 * 用于解析在org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory中使用的断路器名称。
 * @author liyibo
 * @date 2026-05-08 12:25
 */
public interface CircuitBreakerNameResolver {
    String resolveCircuitBreakerName(String feignClientName, Target<?> target, Method method);
}
