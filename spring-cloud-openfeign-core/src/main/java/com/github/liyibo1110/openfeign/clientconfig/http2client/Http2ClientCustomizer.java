package com.github.liyibo1110.openfeign.clientconfig.http2client;

import java.net.http.HttpClient;

/**
 * 回调接口，希望通过HttpClient.Builder进一步自定义HttpClient同时保留其默认自动配置的Bean可以实现该接口。
 * @author liyibo
 * @date 2026-05-07 13:30
 */
@FunctionalInterface
public interface Http2ClientCustomizer {

    void customize(HttpClient.Builder builder);
}
