package com.github.liyibo1110.openfeign;

import feign.MethodMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Feign Contract的方法参数处理器。
 * @author liyibo
 * @date 2026-05-06 13:40
 */
public interface AnnotatedParameterProcessor {

    /**
     * 获取处理器支持的注解类型。
     */
    Class<? extends Annotation> getAnnotationType();

    /**
     * 处理特定注解上面的参数。
     */
    boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method);

    /**
     * 特定的解析参数时的上下文对象。
     */
    interface AnnotatedParameterContext {
        MethodMetadata getMethodMetadata();

        int getParameterIndex();

        void setParameterName(String name);

        Collection<String> setTemplateParameter(String name, Collection<String> rest);
    }
}
