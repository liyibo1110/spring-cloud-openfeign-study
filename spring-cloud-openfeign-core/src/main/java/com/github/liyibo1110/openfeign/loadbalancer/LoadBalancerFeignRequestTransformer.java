package com.github.liyibo1110.openfeign.loadbalancer;

import feign.Request;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.annotation.Order;

/**
 * 允许应用程序根据所选的服务实例对经过负载均衡的请求进行转换
 * @author liyibo
 * @date 2026-05-07 11:07
 */
@Order(LoadBalancerFeignRequestTransformer.DEFAULT_ORDER)
public interface LoadBalancerFeignRequestTransformer {

    int DEFAULT_ORDER = 0;

    Request transformRequest(Request request, ServiceInstance instance);
}
