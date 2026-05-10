package com.github.liyibo1110.openfeign.encoding;

import feign.Feign;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Feign response compression相关Bean配置（FeignContentGzipEncodingInterceptor）。
 * @author liyibo
 * @date 2026-05-10 17:14
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FeignClientEncodingProperties.class)
@ConditionalOnClass(Feign.class)
@Conditional(OkHttpFeignClientBeanMissingCondition.class)
@ConditionalOnProperty("spring.cloud.openfeign.compression.request.enabled")
@AutoConfigureAfter(FeignAutoConfiguration.class)
public class FeignContentGzipEncodingAutoConfiguration {

    @Bean
    public FeignContentGzipEncodingInterceptor feignContentGzipEncodingInterceptor(FeignClientEncodingProperties properties) {
        return new FeignContentGzipEncodingInterceptor(properties);
    }
}
