package com.github.liyibo1110.openfeign;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于创建feign类实例的工厂。
 * 会根据客户端名称创建一个Spring ApplicationContext，并从中提取所需的Bean。
 * @author liyibo
 * @date 2026-05-05 13:46
 */
public class FeignClientFactory extends NamedContextFactory<FeignClientSpecification> {

    public FeignClientFactory() {
        this(new HashMap<>());
    }

    public FeignClientFactory(Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers) {
        super(FeignClientsConfiguration.class, "spring.cloud.openfeign", "spring.cloud.openfeign.client.name",
                applicationContextInitializers);
    }

    @Nullable
    public <T> T getInstanceWithoutAncestors(String name, Class<T> type) {
        try {
            return BeanFactoryUtils.beanOfType(getContext(name), type);
        }
        catch (BeansException ex) {
            return null;
        }
    }

    @Nullable
    public <T> Map<String, T> getInstancesWithoutAncestors(String name, Class<T> type) {
        return getContext(name).getBeansOfType(type);
    }

    public <T> T getInstance(String contextName, String beanName, Class<T> type) {
        return getContext(contextName).getBean(beanName, type);
    }

    @SuppressWarnings("unchecked")
    public FeignClientFactory withApplicationContextInitializers(Map<String, Object> applicationContextInitializers) {
        Map<String, ApplicationContextInitializer<GenericApplicationContext>> convertedInitializers = new HashMap<>();
        applicationContextInitializers.keySet()
                .forEach(contextId -> convertedInitializers.put(contextId,
                        (ApplicationContextInitializer<GenericApplicationContext>) applicationContextInitializers.get(contextId)));
        return new FeignClientFactory(convertedInitializers);
    }
}
