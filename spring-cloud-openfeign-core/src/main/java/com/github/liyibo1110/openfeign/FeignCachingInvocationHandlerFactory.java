package com.github.liyibo1110.openfeign;

import feign.InvocationHandlerFactory;
import feign.Target;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.cache.interceptor.CacheInterceptor;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * 允许在Feign Client的方法上声明Spring的@Cache相关注解。
 * @author liyibo
 * @date 2026-05-08 11:55
 */
public class FeignCachingInvocationHandlerFactory implements InvocationHandlerFactory {

    private final InvocationHandlerFactory delegateFactory;

    private final CacheInterceptor cacheInterceptor;

    public FeignCachingInvocationHandlerFactory(InvocationHandlerFactory delegateFactory, CacheInterceptor cacheInterceptor) {
        this.delegateFactory = delegateFactory;
        this.cacheInterceptor = cacheInterceptor;
    }

    @Override
    public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
        final InvocationHandler delegateHandler = delegateFactory.create(target, dispatch);
        return (proxy, method, argsNullable) -> {
            Object[] args = Optional.ofNullable(argsNullable).orElseGet(() -> new Object[0]);
            return cacheInterceptor.invoke(new MethodInvocation() {
                @Override
                public Method getMethod() {
                    return method;
                }

                @Override
                public Object[] getArguments() {
                    return args;
                }

                @Override
                public Object proceed() throws Throwable {
                    return delegateHandler.invoke(proxy, method, args);
                }

                @Override
                public Object getThis() {
                    return target;
                }

                @Override
                public AccessibleObject getStaticPart() {
                    return method;
                }
            });
        };
    }
}
