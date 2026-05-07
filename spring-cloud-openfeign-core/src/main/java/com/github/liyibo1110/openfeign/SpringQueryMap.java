package com.github.liyibo1110.openfeign;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Spring MVC中与OpenFeign的feign.QueryMap参数注解相对应的功能。
 * @author liyibo
 * @date 2026-05-06 14:43
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface SpringQueryMap {

}
