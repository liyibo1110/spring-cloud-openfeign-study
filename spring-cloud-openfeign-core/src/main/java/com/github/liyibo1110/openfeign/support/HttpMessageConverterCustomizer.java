package com.github.liyibo1110.openfeign.support;

import org.springframework.http.converter.HttpMessageConverter;

import java.util.List;
import java.util.function.Consumer;

/**
 * 允许自定义通过Consumer参数传递的HttpMessageConverter对象
 * @author liyibo
 * @date 2026-05-06 13:45
 */
public interface HttpMessageConverterCustomizer extends Consumer<List<HttpMessageConverter<?>>> {

}
