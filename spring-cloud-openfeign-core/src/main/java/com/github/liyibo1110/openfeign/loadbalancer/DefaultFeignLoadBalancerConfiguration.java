package com.github.liyibo1110.openfeign.loadbalancer;

import feign.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 该配置实例化了一个基于LoadBalancerClient的Client对象，该对象在底层使用Client.Default。
 * @author liyibo
 * @date 2026-05-07 13:21
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LoadBalancerClientsProperties.class)
class DefaultFeignLoadBalancerConfiguration {

    /**
     * 用FeignBlockingLoadBalancerClient。
     */
    @Bean
    @ConditionalOnMissingBean
    @Conditional(OnRetryNotEnabledCondition.class)
    public Client feignClient(LoadBalancerClient loadBalancerClient,
                              LoadBalancerClientFactory loadBalancerClientFactory,
                              List<LoadBalancerFeignRequestTransformer> transformers) {
        return new FeignBlockingLoadBalancerClient(new Client.Default(null, null), loadBalancerClient,
                loadBalancerClientFactory, transformers);
    }

    /**
     * 用RetryableFeignBlockingLoadBalancerClient。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
    @ConditionalOnBean(LoadBalancedRetryFactory.class)
    @ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true", matchIfMissing = true)
    public Client feignRetryClient(LoadBalancerClient loadBalancerClient,
                                   LoadBalancedRetryFactory loadBalancedRetryFactory,
                                   LoadBalancerClientFactory loadBalancerClientFactory,
                                   List<LoadBalancerFeignRequestTransformer> transformers) {
        return new RetryableFeignBlockingLoadBalancerClient(new Client.Default(null, null), loadBalancerClient,
                loadBalancedRetryFactory, loadBalancerClientFactory, transformers);
    }
}
