package com.github.liyibo1110.openfeign.hateoas;

import com.github.liyibo1110.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.hateoas.config.WebConverters;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.List;

/**
 * 为了处理HATEOAS/HAL的特殊对象响应，需要注册额外的WebConverters
 * @author liyibo
 * @date 2026-05-11 12:18
 */
public class WebConvertersCustomizer implements HttpMessageConverterCustomizer {

    private final WebConverters webConverters;

    public WebConvertersCustomizer(WebConverters webConverters) {
        this.webConverters = webConverters;
    }

    @Override
    public void accept(List<HttpMessageConverter<?>> httpMessageConverters) {
        webConverters.augmentClient(httpMessageConverters);
    }
}
