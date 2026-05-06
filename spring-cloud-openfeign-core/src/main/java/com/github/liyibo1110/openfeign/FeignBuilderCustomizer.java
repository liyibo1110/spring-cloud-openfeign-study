package com.github.liyibo1110.openfeign;

import feign.Feign;

/**
 * 允许自定义Feign Builder。
 * @author liyibo
 * @date 2026-05-06 10:25
 */
@FunctionalInterface
public interface FeignBuilderCustomizer {

    void customize(Feign.Builder builder);
}
