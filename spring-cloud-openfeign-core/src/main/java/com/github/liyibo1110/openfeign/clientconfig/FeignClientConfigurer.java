package com.github.liyibo1110.openfeign.clientconfig;

/**
 * FeignClient中未包含的其他FeignClient配置。
 * @author liyibo
 * @date 2026-05-06 11:37
 */
public interface FeignClientConfigurer {

    /**
     * 是否将feign代理标记为主Bean，默认值为true。
     */
    default boolean primary() {
        return true;
    }

    /**
     * 默认值为true，如果设置为false，则仅应用configuration()中列出的类的配置。
     * 如果未提供feign.codec.Decoder、feign.codec.Encoder和feign.Contract的实例，仍将使用其父类。
     */
    default boolean inheritParentConfiguration() {
        return true;
    }
}
