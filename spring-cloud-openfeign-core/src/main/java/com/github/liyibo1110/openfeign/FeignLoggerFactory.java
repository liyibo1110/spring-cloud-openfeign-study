package com.github.liyibo1110.openfeign;

import feign.Logger;

/**
 * 允许使用自定义的Feign日志记录器。
 * @author liyibo
 * @date 2026-05-06 11:27
 */
public interface FeignLoggerFactory {

    Logger create(Class<?> type);
}
