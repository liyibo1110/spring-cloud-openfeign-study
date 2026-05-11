package com.github.liyibo1110.openfeign.hateoas;

import com.github.liyibo1110.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.HateoasConfiguration;
import org.springframework.hateoas.config.WebConverters;

/**
 * @author liyibo
 * @date 2026-05-11 12:19
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@ConditionalOnClass(WebConverters.class)
@AutoConfigureAfter({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        RepositoryRestMvcAutoConfiguration.class, HateoasConfiguration.class })
public class FeignHalAutoConfiguration {
    @Bean
    @ConditionalOnBean(WebConverters.class)
    HttpMessageConverterCustomizer webConvertersCustomizer(WebConverters webConverters) {
        return new WebConvertersCustomizer(webConverters);
    }
}
