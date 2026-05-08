package com.github.liyibo1110.openfeign.loadbalancer;

import feign.Client;
import feign.http2client.Http2Client;
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

import java.net.http.HttpClient;
import java.util.List;

/**
 * 生成基于JDK HttpClient的Client对象，内部又分为带retry和不带retry的版本。
 * @author liyibo
 * @date 2026-05-07 13:38
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Http2Client.class, HttpClient.class })
@ConditionalOnBean({ LoadBalancerClient.class, LoadBalancerClientFactory.class })
@ConditionalOnProperty("spring.cloud.openfeign.http2client.enabled")
@EnableConfigurationProperties(LoadBalancerClientsProperties.class)
class Http2ClientFeignLoadBalancerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Conditional(OnRetryNotEnabledCondition.class)
    public Client feignClient(LoadBalancerClient loadBalancerClient,
                              HttpClient httpClient,
                              LoadBalancerClientFactory loadBalancerClientFactory,
                              List<LoadBalancerFeignRequestTransformer> transformers) {
        Client delegate = new Http2Client(httpClient);
        return new FeignBlockingLoadBalancerClient(delegate, loadBalancerClient, loadBalancerClientFactory, transformers);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
    @ConditionalOnBean(LoadBalancedRetryFactory.class)
    @ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true",
            matchIfMissing = true)
    public Client feignRetryClient(LoadBalancerClient loadBalancerClient,
                                   HttpClient httpClient,
                                   LoadBalancedRetryFactory loadBalancedRetryFactory,
                                   LoadBalancerClientFactory loadBalancerClientFactory,
                                   List<LoadBalancerFeignRequestTransformer> transformers) {
        Client delegate = new Http2Client(httpClient);
        return new RetryableFeignBlockingLoadBalancerClient(delegate, loadBalancerClient, loadBalancedRetryFactory,
                loadBalancerClientFactory, transformers);
    }
}
