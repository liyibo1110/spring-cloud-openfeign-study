package com.github.liyibo1110.openfeign;

import feign.Logger;
import feign.slf4j.Slf4jLogger;

/**
 * @author liyibo
 * @date 2026-05-06 14:46
 */
public class DefaultFeignLoggerFactory implements FeignLoggerFactory {

    private final Logger logger;

    public DefaultFeignLoggerFactory(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Logger create(Class<?> type) {
        return this.logger != null ? this.logger : new Slf4jLogger(type);
    }
}
