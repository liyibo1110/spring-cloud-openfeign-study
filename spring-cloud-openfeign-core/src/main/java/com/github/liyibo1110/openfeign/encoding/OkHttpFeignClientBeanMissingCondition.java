package com.github.liyibo1110.openfeign.encoding;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 用于验证是否未满足创建Feign Client Bean的条件（该Bean自身为OkHttpClient类型，或其委托对象为OkHttpClient类型）的Condition。
 * @author liyibo
 * @date 2026-05-10 16:34
 */
public class OkHttpFeignClientBeanMissingCondition extends AnyNestedCondition {

    public OkHttpFeignClientBeanMissingCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnMissingClass("feign.okhttp.OkHttpClient")
    static class FeignOkHttpClientPresent {

    }

    @ConditionalOnProperty(value = "spring.cloud.openfeign.okhttp.enabled", havingValue = "false")
    static class FeignOkHttpClientEnabled {

    }
}
