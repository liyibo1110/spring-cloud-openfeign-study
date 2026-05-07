package com.github.liyibo1110.openfeign.support;

import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * Feign相关工具类
 * @author liyibo
 * @date 2026-05-06 13:43
 */
public final class FeignUtils {

    private FeignUtils() {
        throw new IllegalStateException("Can't instantiate a utility class");
    }

    /**
     * 填充并生成HttpHeaders对象。
     */
    static HttpHeaders getHttpHeaders(Map<String, Collection<String>> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        for (Map.Entry<String, Collection<String>> entry : headers.entrySet())
            httpHeaders.put(entry.getKey(), new ArrayList<>(entry.getValue()));

        return httpHeaders;
    }

    /**
     * 增加特定的新paramName。
     */
    static Collection<String> addTemplateParameter(Collection<String> possiblyNull, String paramName) {
        Collection<String> params = ofNullable(possiblyNull).map(ArrayList::new).orElse(new ArrayList<>());
        params.add(String.format("{%s}", paramName));
        return params;
    }
}
