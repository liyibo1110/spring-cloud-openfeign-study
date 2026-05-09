package com.github.liyibo1110.openfeign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static feign.Util.checkNotNull;

/**
 * 生成Fallback对象的工厂。
 * @author liyibo
 * @date 2026-05-08 11:33
 */
@FunctionalInterface
public interface FallbackFactory<T> {

    /**
     * 返回一个适用于给定cause的Fallback实例。
     */
    T create(Throwable cause);

    final class Default<T> implements FallbackFactory<T> {
        final T constant;
        final Log logger;

        public Default(T constant) {
            this(constant, LogFactory.getLog(Default.class));
        }

        Default(T constant, Log logger) {
            this.constant = checkNotNull(constant, "fallback");
            this.logger = checkNotNull(logger, "logger");
        }

        @Override
        public T create(Throwable cause) {
            if (logger.isTraceEnabled())
                logger.trace("fallback due to: " + cause.getMessage(), cause);
            return constant;
        }

        @Override
        public String toString() {
            return constant.toString();
        }
    }
}
