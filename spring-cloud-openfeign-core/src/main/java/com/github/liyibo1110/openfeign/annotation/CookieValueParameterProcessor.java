package com.github.liyibo1110.openfeign.annotation;

import com.github.liyibo1110.openfeign.AnnotatedParameterProcessor;
import feign.MethodMetadata;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * CookieValue注解处理器，解析里面的字段，然后写入RequestTemplate。
 * @author liyibo
 * @date 2026-05-07 10:12
 */
public class CookieValueParameterProcessor implements AnnotatedParameterProcessor {

    private static final Class<CookieValue> ANNOTATION = CookieValue.class;

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return ANNOTATION;
    }

    @Override
    public boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method) {
        int parameterIndex = context.getParameterIndex();
        MethodMetadata data = context.getMethodMetadata();
        CookieValue cookie = ANNOTATION.cast(annotation);
        String name = cookie.value().trim();
        checkState(emptyToNull(name) != null, "Cookie.name() was empty on parameter %s", parameterIndex);
        context.setParameterName(name);
        // 先找旧的cookie header，有就追加新的
        String cookieExpression = data.template()
                .headers()
                .getOrDefault(HttpHeaders.COOKIE, Collections.singletonList(""))
                .stream()
                .findFirst()
                .orElse("");
        if (cookieExpression.length() == 0)
            cookieExpression = String.format("%s={%s}", name, name);
        else
            cookieExpression += String.format("; %s={%s}", name, name);

        data.template().removeHeader(HttpHeaders.COOKIE);
        data.template().header(HttpHeaders.COOKIE, cookieExpression);
        return true;
    }
}
