package com.github.liyibo1110.openfeign.loadbalancer;

import com.github.liyibo1110.openfeign.support.FeignHttpClientProperties;
import feign.Feign;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author liyibo
 * @date 2026-05-07 13:47
 */
@ConditionalOnClass(Feign.class)
@ConditionalOnBean({ LoadBalancerClient.class, LoadBalancerClientFactory.class })
@AutoConfigureBefore(FeignAutoConfiguration.class)
@AutoConfigureAfter({ BlockingLoadBalancerClientAutoConfiguration.class, LoadBalancerAutoConfiguration.class })
@EnableConfigurationProperties(FeignHttpClientProperties.class)
@Configuration(proxyBeanMethods = false)
@Import({ OkHttpFeignLoadBalancerConfiguration.class, HttpClient5FeignLoadBalancerConfiguration.class,
        Http2ClientFeignLoadBalancerConfiguration.class, DefaultFeignLoadBalancerConfiguration.class })
public class FeignLoadBalancerAutoConfiguration {

    @Bean
    @ConditionalOnBean(LoadBalancerClientFactory.class)
    @ConditionalOnMissingBean(XForwardedHeadersTransformer.class)
    public XForwardedHeadersTransformer xForwarderHeadersFeignTransformer(LoadBalancerClientFactory factory) {
        return new XForwardedHeadersTransformer(factory);
    }
}
