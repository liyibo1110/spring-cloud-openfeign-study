package com.github.liyibo1110.openfeign;

import feign.Feign;
import feign.Target;

/**
 * Targeter接口的默认实现，直接调用Feign Builder的默认target方法。
 * @author liyibo
 * @date 2026-05-08 10:49
 */
public class DefaultTargeter implements Targeter {

    @Override
    public <T> T target(FeignClientFactoryBean factory,
                        Feign.Builder feign,
                        FeignClientFactory context,
                        Target.HardCodedTarget<T> target) {
        return feign.target(target);
    }
}
