package com.github.liyibo1110.openfeign;

import org.springframework.context.ApplicationContext;

/**
 * 一个用于在不使用FeignClient注解的情况下创建Feign客户端的构建器。
 * 该构建器生成的FeignClient与使用FeignClient注解创建的完全一致。
 * @author liyibo
 * @date 2026-05-08 12:14
 */
public class FeignClientBuilder {

    private final ApplicationContext applicationContext;

    public FeignClientBuilder(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T> Builder<T> forType(final Class<T> type, final String name) {
        return new Builder<>(this.applicationContext, type, name);
    }

    public <T> Builder<T> forType(final Class<T> type, final FeignClientFactoryBean clientFactoryBean, final String name) {
        return new Builder<>(this.applicationContext, clientFactoryBean, type, name);
    }

    public static final class Builder<T> {
        private final FeignClientFactoryBean feignClientFactoryBean;

        private Builder(final ApplicationContext applicationContext, final Class<T> type, final String name) {
            this(applicationContext, new FeignClientFactoryBean(), type, name);
        }

        private Builder(final ApplicationContext applicationContext, final FeignClientFactoryBean clientFactoryBean,
                        final Class<T> type, final String name) {
            this.feignClientFactoryBean = clientFactoryBean;

            this.feignClientFactoryBean.setApplicationContext(applicationContext);
            this.feignClientFactoryBean.setType(type);
            this.feignClientFactoryBean.setName(FeignClientsRegistrar.getName(name));
            this.feignClientFactoryBean.setContextId(FeignClientsRegistrar.getName(name));
            this.feignClientFactoryBean.setInheritParentContext(true);
            // preset default values - these values resemble the default values on the
            // FeignClient annotation
            this.url("").path("").dismiss404(false);
        }

        public Builder<T> url(final String url) {
            this.feignClientFactoryBean.setUrl(FeignClientsRegistrar.getUrl(url));
            return this;
        }

        public Builder<T> customize(final FeignBuilderCustomizer customizer) {
            this.feignClientFactoryBean.addCustomizer(customizer);
            return this;
        }

        public Builder<T> contextId(final String contextId) {
            this.feignClientFactoryBean.setContextId(contextId);
            return this;
        }

        public Builder<T> path(final String path) {
            this.feignClientFactoryBean.setPath(FeignClientsRegistrar.getPath(path));
            return this;
        }

        public Builder<T> dismiss404(final boolean dismiss404) {
            this.feignClientFactoryBean.setDismiss404(dismiss404);
            return this;
        }

        public Builder<T> inheritParentContext(final boolean inheritParentContext) {
            this.feignClientFactoryBean.setInheritParentContext(inheritParentContext);
            return this;
        }

        public Builder<T> fallback(final Class<? extends T> fallback) {
            FeignClientsRegistrar.validateFallback(fallback);
            this.feignClientFactoryBean.setFallback(fallback);
            return this;
        }

        public T build() {
            return this.feignClientFactoryBean.getTarget();
        }
    }
}
