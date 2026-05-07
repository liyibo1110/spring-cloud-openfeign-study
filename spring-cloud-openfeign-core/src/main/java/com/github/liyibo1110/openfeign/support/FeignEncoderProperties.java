package com.github.liyibo1110.openfeign.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SpringEncoder相关的配置属性
 * @author liyibo
 * @date 2026-05-06 13:51
 */
@ConfigurationProperties("spring.cloud.openfeign.encoder")
public class FeignEncoderProperties {

    private boolean charsetFromContentType = false;

    public boolean isCharsetFromContentType() {
        return charsetFromContentType;
    }

    public void setCharsetFromContentType(boolean charsetFromContentType) {
        this.charsetFromContentType = charsetFromContentType;
    }
}
