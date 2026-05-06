package com.github.liyibo1110.openfeign;

/**
 * 该组件将url封装在一个对象中，以便能够使用RefreshableUrlFactoryBean创建相应的代理实例。
 * @author liyibo
 * @date 2026-05-06 12:50
 */
public class RefreshableUrl {

    private final String url;

    public RefreshableUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
