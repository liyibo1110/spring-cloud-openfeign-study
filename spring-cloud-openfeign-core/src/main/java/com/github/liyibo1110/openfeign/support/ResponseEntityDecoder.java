package com.github.liyibo1110.openfeign.support;

import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;

/**
 * 解码器通过组合模式，为任何其他解码器添加了对Spring MVC的ResponseEntity的兼容性。
 * @author liyibo
 * @date 2026-05-06 14:24
 */
public class ResponseEntityDecoder implements Decoder {
    private final Decoder decoder;

    public ResponseEntityDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Object decode(final Response response, Type type) throws IOException, FeignException {
        if (isParameterizeHttpEntity(type)) {
            type = ((ParameterizedType) type).getActualTypeArguments()[0];
            Object decodedObject = this.decoder.decode(response, type);
            return createResponse(decodedObject, response);
        } else if (isHttpEntity(type)) {
            return createResponse(null, response);
        } else {
            return this.decoder.decode(response, type);
        }
    }

    private boolean isParameterizeHttpEntity(Type type) {
        if (type instanceof ParameterizedType)
            return isHttpEntity(((ParameterizedType) type).getRawType());

        return false;
    }

    private boolean isHttpEntity(Type type) {
        if (type instanceof Class c)
            return HttpEntity.class.isAssignableFrom(c);

        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<T> createResponse(Object instance, Response response) {
        HttpHeaders headers = new HttpHeaders();
        for (String key : response.headers().keySet())
            headers.put(key, new LinkedList<>(response.headers().get(key)));

        return new ResponseEntity<>((T) instance, headers, HttpStatusCode.valueOf(response.status()));
    }
}
