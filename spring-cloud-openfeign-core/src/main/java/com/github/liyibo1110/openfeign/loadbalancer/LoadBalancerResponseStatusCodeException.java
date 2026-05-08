package com.github.liyibo1110.openfeign.loadbalancer;

import feign.Response;
import org.springframework.cloud.client.loadbalancer.RetryableStatusCodeException;

import java.io.ByteArrayInputStream;
import java.net.URI;

/**
 * @author liyibo
 * @date 2026-05-07 13:46
 */
public class LoadBalancerResponseStatusCodeException extends RetryableStatusCodeException {

    private final Response response;

    public LoadBalancerResponseStatusCodeException(String serviceId, Response response, byte[] body, URI uri) {
        super(serviceId, response.status(), response, uri);
        this.response = Response.builder()
                .body(new ByteArrayInputStream(body), body.length)
                .headers(response.headers())
                .reason(response.reason())
                .status(response.status())
                .request(response.request())
                .build();
    }

    @Override
    public Response getResponse() {
        return this.response;
    }
}
