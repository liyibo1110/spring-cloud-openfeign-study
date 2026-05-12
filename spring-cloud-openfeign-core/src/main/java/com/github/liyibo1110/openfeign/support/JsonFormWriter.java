package com.github.liyibo1110.openfeign.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * AbstractFormWriter的Json实现，基于jackson
 * @author liyibo
 * @date 2026-05-12 12:07
 */
public class JsonFormWriter extends AbstractFormWriter {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected MediaType getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected String writeAsString(Object object) throws IOException {
        return objectMapper.writeValueAsString(object);
    }
}
