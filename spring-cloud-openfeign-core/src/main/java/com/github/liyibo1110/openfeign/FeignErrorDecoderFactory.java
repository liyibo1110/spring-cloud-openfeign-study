package com.github.liyibo1110.openfeign;

import feign.codec.ErrorDecoder;

/**
 * 允许使用自定义的Feign ErrorDecoder。
 * @author liyibo
 * @date 2026-05-06 11:46
 */
public interface FeignErrorDecoderFactory {

    ErrorDecoder create(Class<?> type);
}
