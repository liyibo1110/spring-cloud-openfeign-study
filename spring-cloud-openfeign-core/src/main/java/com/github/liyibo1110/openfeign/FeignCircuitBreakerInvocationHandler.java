package com.github.liyibo1110.openfeign;

import feign.InvocationHandlerFactory;
import feign.Target;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static feign.Util.checkNotNull;

/**
 * Feign + CircuitBreaker的InvocationHandler实现（InvocationHandler可以看作是Dispatcher组件）。
 * @author liyibo
 * @date 2026-05-08 13:04
 */
class FeignCircuitBreakerInvocationHandler implements InvocationHandler {

    private final CircuitBreakerFactory factory;

    private final String feignClientName;

    private final Target<?> target;

    private final Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;

    private final FallbackFactory<?> nullableFallbackFactory;

    private final Map<Method, Method> fallbackMethodMap;

    private final boolean circuitBreakerGroupEnabled;

    private final CircuitBreakerNameResolver circuitBreakerNameResolver;

    FeignCircuitBreakerInvocationHandler(CircuitBreakerFactory factory,
                                         String feignClientName, Target<?> target,
                                         Map<Method, InvocationHandlerFactory.MethodHandler> dispatch,
                                         FallbackFactory<?> nullableFallbackFactory,
                                         boolean circuitBreakerGroupEnabled,
                                         CircuitBreakerNameResolver circuitBreakerNameResolver) {
        this.factory = factory;
        this.feignClientName = feignClientName;
        this.target = checkNotNull(target, "target");
        this.dispatch = checkNotNull(dispatch, "dispatch");
        this.fallbackMethodMap = toFallbackMethod(dispatch);
        this.nullableFallbackFactory = nullableFallbackFactory;
        this.circuitBreakerGroupEnabled = circuitBreakerGroupEnabled;
        this.circuitBreakerNameResolver = circuitBreakerNameResolver;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) {
        // early exit if the invoked method is from java.lang.Object
        // code is the same as ReflectiveFeign.FeignInvocationHandler
        if ("equals".equals(method.getName())) {
            try {
                Object otherHandler = args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
                return equals(otherHandler);
            } catch (IllegalArgumentException e) {
                return false;
            }
        } else if ("hashCode".equals(method.getName())) {
            return hashCode();
        } else if ("toString".equals(method.getName())) {
            return toString();
        }

        String circuitName = circuitBreakerNameResolver.resolveCircuitBreakerName(feignClientName, target, method);
        CircuitBreaker circuitBreaker = circuitBreakerGroupEnabled
                ? factory.create(circuitName, feignClientName)
                : factory.create(circuitName);
        Supplier<Object> supplier = asSupplier(method, args);
        if (this.nullableFallbackFactory != null) {
            Function<Throwable, Object> fallbackFunction = throwable -> {
                Object fallback = this.nullableFallbackFactory.create(throwable);
                try {
                    return this.fallbackMethodMap.get(method).invoke(fallback, args);
                }
                catch (Exception exception) {
                    unwrapAndRethrow(exception);
                }
                return null;
            };
            return circuitBreaker.run(supplier, fallbackFunction);
        }
        return circuitBreaker.run(supplier);
    }

    private void unwrapAndRethrow(Exception exception) {
        if (exception instanceof InvocationTargetException || exception instanceof NoFallbackAvailableException) {
            Throwable underlyingException = exception.getCause();
            if (underlyingException instanceof RuntimeException)
                throw (RuntimeException) underlyingException;

            if (underlyingException != null)
                throw new IllegalStateException(underlyingException);

            throw new IllegalStateException(exception);
        }
    }

    private Supplier<Object> asSupplier(final Method method, final Object[] args) {
        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        final Thread caller = Thread.currentThread();
        return () -> {
            boolean isAsync = caller != Thread.currentThread();
            try {
                if (isAsync)
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                return dispatch.get(method).invoke(args);
            } catch (RuntimeException t) {
                throw t;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            } finally {
                if (isAsync)
                    RequestContextHolder.resetRequestAttributes();
            }
        };
    }

    /**
     * 如果InvocationHandler.invoke(Object, Method, Object[])方法中的Method参数不可访问（例如位于包私有接口中），则fallback调用将因访问限制而失败。
     * 但dispatch中的方法是复制的方法。
     * 因此，对dispatch方法设置的访问权限不会影响InvocationHandler.invoke中的方法。
     * 使用map存储要调用的方法的副本以触发fallback，即可绕过此限制并减少反射调用的次数。
     */
    static Map<Method, Method> toFallbackMethod(Map<Method, InvocationHandlerFactory.MethodHandler> dispatch) {
        Map<Method, Method> result = new LinkedHashMap<>();
        for (Method method : dispatch.keySet()) {
            method.setAccessible(true);
            result.put(method, method);
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FeignCircuitBreakerInvocationHandler other)
            return this.target.equals(other.target);
        return false;
    }

    @Override
    public int hashCode() {
        return this.target.hashCode();
    }

    @Override
    public String toString() {
        return this.target.toString();
    }
}
