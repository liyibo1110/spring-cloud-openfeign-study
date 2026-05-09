package com.github.liyibo1110.openfeign;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 生成RefreshableUrl bean的factory bean。
 * @author liyibo
 * @date 2026-05-08 11:10
 */
public class RefreshableUrlFactoryBean implements FactoryBean<RefreshableUrl>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private String contextId;

    private RefreshableUrl refreshableUrl;

    @Override
    public Class<?> getObjectType() {
        return RefreshableUrl.class;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public RefreshableUrl getObject() {
        if (refreshableUrl != null)
            return refreshableUrl;

        // 对应spring.cloud.openfeign.client.xxx
        FeignClientProperties properties = applicationContext.getBean(FeignClientProperties.class);
        if (Objects.isNull(properties.getConfig()))
            return new RefreshableUrl(null);

        // 对应spring.cloud.openfeign.client.contextId
        FeignClientProperties.FeignClientConfiguration config = properties.getConfig().get(contextId);
        if (Objects.isNull(config) || !StringUtils.hasText(config.getUrl()))
            return new RefreshableUrl(null);

        // 有对应配置的url
        refreshableUrl = new RefreshableUrl(FeignClientsRegistrar.getUrl(config.getUrl()));
        return refreshableUrl;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

}
