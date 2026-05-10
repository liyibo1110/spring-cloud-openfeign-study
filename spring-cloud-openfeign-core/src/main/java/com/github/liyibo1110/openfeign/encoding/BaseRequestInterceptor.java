package com.github.liyibo1110.openfeign.encoding;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.util.Assert;

/**
 * request interceptor的基类，引入了FeignClientEncodingProperties
 * @author liyibo
 * @date 2026-05-10 16:32
 */
public abstract class BaseRequestInterceptor implements RequestInterceptor {

    private final FeignClientEncodingProperties properties;

    protected BaseRequestInterceptor(FeignClientEncodingProperties properties) {
        Assert.notNull(properties, "Properties can not be null");
        this.properties = properties;
    }

    /**
     * 如果尚未指定，则添加该header
     */
    protected void addHeader(RequestTemplate requestTemplate, String name, String... values) {
        if (!requestTemplate.headers().containsKey(name))
            requestTemplate.header(name, values);
    }

    protected FeignClientEncodingProperties getProperties() {
        return this.properties;
    }
}
