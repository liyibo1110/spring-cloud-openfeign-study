package com.github.liyibo1110.openfeign.loadbalancer;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;

/**
 * 用于验证RetryTemplate是否位于类路径中，是否存在LoadBalancedRetryFactory Bean，
 * 以及spring.cloud.loadbalancer.retry.enabled是否未设置为false。
 * @author liyibo
 * @date 2026-05-07 13:25
 */
public class OnRetryNotEnabledCondition extends AnyNestedCondition {

    public OnRetryNotEnabledCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnMissingClass("org.springframework.retry.support.RetryTemplate")
    static class OnNoRetryTemplateCondition {

    }

    @ConditionalOnMissingBean(LoadBalancedRetryFactory.class)
    static class OnRetryFactoryCondition {

    }

    @ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "false")
    static class OnLoadBalancerRetryEnabledCondition {

    }
}
