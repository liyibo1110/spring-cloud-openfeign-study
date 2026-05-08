package com.github.liyibo1110.openfeign.loadbalancer;

import feign.Request;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 增加X-Forwarded-Host和X-Forwarded-Proto的header。
 * @author liyibo
 * @date 2026-05-07 13:41
 */
public class XForwardedHeadersTransformer implements LoadBalancerFeignRequestTransformer {

    private final ReactiveLoadBalancer.Factory<ServiceInstance> factory;

    public XForwardedHeadersTransformer(ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
        this.factory = factory;
    }

    @Override
    public Request transformRequest(Request request, ServiceInstance instance) {
        if (instance == null)
            return request;

        LoadBalancerProperties.XForwarded xForwarded = factory.getProperties(instance.getServiceId()).getXForwarded();
        if (xForwarded.isEnabled()) {
            Map<String, Collection<String>> headers = new HashMap<>(request.headers());
            URI uri = URI.create(request.url());
            String xForwardedHost = uri.getHost();
            String xForwardedProto = uri.getScheme();
            headers.put("X-Forwarded-Host", Collections.singleton(xForwardedHost));
            headers.put("X-Forwarded-Proto", Collections.singleton(xForwardedProto));
            request = Request.create(request.httpMethod(), request.url(), headers, request.body(), request.charset(), request.requestTemplate());
        }
        return request;
    }
}
