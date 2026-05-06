package com.github.liyibo1110.openfeign;

import feign.Feign;
import feign.Target;

/**
 * 最终target创建策略接口，因为可能要做扩展功能，例如：
 * 1、带CircuitBreaker的Feign代理。
 * 2、带fallback的Feign代理。
 * 3、带fallbackFactory的Feign代理。
 * @author liyibo
 * @date 2026-05-06 11:22
 */
public interface Targeter {

    <T> T target(FeignClientFactoryBean factory,
                 Feign.Builder feign,
                 FeignClientFactory context,
                 Target.HardCodedTarget<T> target);
}
