package com.github.liyibo1110.openfeign.clientconfig;

import com.github.liyibo1110.openfeign.clientconfig.http2client.Http2ClientCustomizer;
import com.github.liyibo1110.openfeign.support.FeignHttpClientProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * 负责生成JDK HttpClient的Configuration。
 * @author liyibo
 * @date 2026-05-07 13:34
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(HttpClient.class)
public class Http2ClientFeignConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HttpClient.Builder httpClientBuilder(FeignHttpClientProperties httpClientProperties) {
        return HttpClient.newBuilder()
                .followRedirects(httpClientProperties.isFollowRedirects() ? HttpClient.Redirect.ALWAYS : HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.valueOf(httpClientProperties.getHttp2().getVersion()))
                .connectTimeout(Duration.ofMillis(httpClientProperties.getConnectionTimeout()));
    }

    @Bean
    public HttpClient httpClient(HttpClient.Builder httpClientBuilder, List<Http2ClientCustomizer> customizers) {
        customizers.forEach(customizer -> customizer.customize(httpClientBuilder));
        return httpClientBuilder.build();
    }
}
