package com.github.liyibo1110.openfeign.annotation;

import com.github.liyibo1110.openfeign.AnnotatedParameterProcessor;
import feign.MethodMetadata;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * PathVariable注解处理器。
 * @author liyibo
 * @date 2026-05-07 10:24
 */
public class PathVariableParameterProcessor implements AnnotatedParameterProcessor {

    private static final Class<PathVariable> ANNOTATION = PathVariable.class;

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return ANNOTATION;
    }

    @Override
    public boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method) {
        String name = ANNOTATION.cast(annotation).value();
        checkState(emptyToNull(name) != null, "PathVariable annotation was empty on param %s.",
                context.getParameterIndex());
        context.setParameterName(name);

        MethodMetadata data = context.getMethodMetadata();
        String varName = '{' + name + '}';
        String varNameRegex = ".*\\{" + name + "(:[^}]+)?\\}.*";
        if (!data.template().url().matches(varNameRegex) && !containsMapValues(data.template().queries(), varName)
                && !containsMapValues(data.template().headers(), varName)) {
            data.formParams().add(name);
        }
        return true;
    }

    private <K, V> boolean containsMapValues(Map<K, Collection<V>> map, V search) {
        Collection<Collection<V>> values = map.values();
        if (values == null)
            return false;

        for (Collection<V> entry : values) {
            if (entry.contains(search))
                return true;
        }
        return false;
    }
}
