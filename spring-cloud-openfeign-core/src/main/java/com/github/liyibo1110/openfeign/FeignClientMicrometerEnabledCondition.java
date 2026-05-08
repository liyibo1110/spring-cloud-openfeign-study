package com.github.liyibo1110.openfeign;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * @author liyibo
 * @date 2026-05-07 13:49
 */
class FeignClientMicrometerEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        FeignClientProperties feignClientProperties = context.getBeanFactory()
                .getBeanProvider(FeignClientProperties.class)
                .getIfAvailable();
        if (feignClientProperties != null) {
            Map<String, FeignClientProperties.FeignClientConfiguration> feignClientConfigMap = feignClientProperties.getConfig();
            if (feignClientConfigMap != null) {
                FeignClientProperties.FeignClientConfiguration feignClientConfig = feignClientConfigMap
                        .get(context.getEnvironment().getProperty("spring.cloud.openfeign.client.name"));
                if (feignClientConfig != null) {
                    FeignClientProperties.MicrometerProperties micrometer = feignClientConfig.getMicrometer();
                    if (micrometer != null && micrometer.getEnabled() != null)
                        return micrometer.getEnabled();
                }
            }
        }

        return true;
    }
}
