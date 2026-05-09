package com.github.liyibo1110.openfeign;

import feign.Target;

/**
 * 在首次调用时根据属性解析URL。
 * 通过设置 spring.cloud.openfeign.client.config.[clientId].url的值，可以在AOT打包的应用程序或原生映像中于运行时指定该URL。
 * @author liyibo
 * @date 2026-05-08 11:04
 */
public class PropertyBasedTarget<T> extends Target.HardCodedTarget<T> {

    private String url;

    private final FeignClientProperties.FeignClientConfiguration config;

    private final String path;

    public PropertyBasedTarget(Class<T> type, String name, FeignClientProperties.FeignClientConfiguration config, String path) {
        super(type, name, config.getUrl());
        this.config = config;
        this.path = path;
    }

    public PropertyBasedTarget(Class<T> type, String name, FeignClientProperties.FeignClientConfiguration config) {
        super(type, name, config.getUrl());
        this.config = config;
        this.path = "";
    }

    @Override
    public String url() {
        if (url == null)
            url = config.getUrl() + path;
        return url;
    }
}
