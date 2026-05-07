package com.github.liyibo1110.openfeign;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示在处理注解方法时应使用哪种收集格式。
 * @author liyibo
 * @date 2026-05-06 15:14
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionFormat {
    feign.CollectionFormat value();
}
