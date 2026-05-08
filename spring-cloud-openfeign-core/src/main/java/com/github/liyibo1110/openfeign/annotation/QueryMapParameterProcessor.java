package com.github.liyibo1110.openfeign.annotation;

import com.github.liyibo1110.openfeign.AnnotatedParameterProcessor;
import com.github.liyibo1110.openfeign.SpringQueryMap;
import feign.MethodMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * SpringQueryMap注解处理器。
 * @author liyibo
 * @date 2026-05-07 10:27
 */
public class QueryMapParameterProcessor implements AnnotatedParameterProcessor {

    private static final Class<SpringQueryMap> ANNOTATION = SpringQueryMap.class;

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return ANNOTATION;
    }

    @Override
    public boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method) {
        int paramIndex = context.getParameterIndex();
        MethodMetadata metadata = context.getMethodMetadata();
        if (metadata.queryMapIndex() == null)
            metadata.queryMapIndex(paramIndex);
        return true;
    }
}
