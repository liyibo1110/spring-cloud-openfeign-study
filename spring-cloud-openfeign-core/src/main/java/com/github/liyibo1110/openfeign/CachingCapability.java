package com.github.liyibo1110.openfeign;

import feign.Capability;
import feign.InvocationHandlerFactory;
import org.springframework.cache.interceptor.CacheInterceptor;

/**
 * 允许在Feign Client的方法上声明Spring的@Cache相关注解。
 * @author liyibo
 * @date 2026-05-08 11:51
 */
public class CachingCapability implements Capability {

    private final CacheInterceptor cacheInterceptor;

    public CachingCapability(CacheInterceptor cacheInterceptor) {
        this.cacheInterceptor = cacheInterceptor;
    }

    @Override
    public InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory) {
        return new FeignCachingInvocationHandlerFactory(invocationHandlerFactory, cacheInterceptor);
    }
}
