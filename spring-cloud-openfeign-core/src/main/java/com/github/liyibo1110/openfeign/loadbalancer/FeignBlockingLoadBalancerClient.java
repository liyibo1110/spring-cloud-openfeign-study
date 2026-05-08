package com.github.liyibo1110.openfeign.loadbalancer;

import feign.Client;
import feign.Request;
import feign.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycleValidator;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 把http://user-service/api/users/1这样的，转换成http://192.168.1.10:8080/api/users/1。
 * 本身是个装饰器。
 * @author liyibo
 * @date 2026-05-07 11:03
 */
public class FeignBlockingLoadBalancerClient implements Client {
    private static final Log LOG = LogFactory.getLog(FeignBlockingLoadBalancerClient.class);

    /** 负责真正发http请求，可以有各种底层实现 */
    private final Client delegate;

    /** 负责选择服务实例，即ServiceInstance，从Nacos、Eureka、Consul之类的注册中心而来 */
    private final LoadBalancerClient loadBalancerClient;

    /**
     * 负责拿负载均衡相关上下文和配置：
     * 1、拿当前serviceId对应的生命周期处理器。
     * 2、拿当前serviceId对应的LoadBalancer配置。
     * 类似FeignClientFactory，只是把contextId换成了serviceId
     */
    private final LoadBalancerClientFactory loadBalancerClientFactory;

    /**
     * 扩展点，可以把重建后的真实URL，继续进行自定义修改，例如：
     * 1、给请求增加实例信息header。
     * 2、根据ServiceInstance metadata改造请求。
     * 3、做灰度、区域、版本等标记透传。
     */
    private final List<LoadBalancerFeignRequestTransformer> transformers;

    @Deprecated(forRemoval = true)
    public FeignBlockingLoadBalancerClient(Client delegate,
                                           LoadBalancerClient loadBalancerClient,
                                           LoadBalancerProperties properties,
                                           LoadBalancerClientFactory loadBalancerClientFactory) {
        this.delegate = delegate;
        this.loadBalancerClient = loadBalancerClient;
        this.loadBalancerClientFactory = loadBalancerClientFactory;
        this.transformers = Collections.emptyList();
    }

    @Deprecated(forRemoval = true)
    public FeignBlockingLoadBalancerClient(Client delegate,
                                           LoadBalancerClient loadBalancerClient,
                                           LoadBalancerClientFactory loadBalancerClientFactory) {
        this.delegate = delegate;
        this.loadBalancerClient = loadBalancerClient;
        this.loadBalancerClientFactory = loadBalancerClientFactory;
        this.transformers = Collections.emptyList();
    }

    public FeignBlockingLoadBalancerClient(Client delegate,
                                           LoadBalancerClient loadBalancerClient,
                                           LoadBalancerClientFactory loadBalancerClientFactory,
                                           List<LoadBalancerFeignRequestTransformer> transformers) {
        this.delegate = delegate;
        this.loadBalancerClient = loadBalancerClient;
        this.loadBalancerClientFactory = loadBalancerClientFactory;
        this.transformers = transformers;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        final URI originalUri = URI.create(request.url());  // originalUri = http://user-service/api/users/1
        String serviceId = originalUri.getHost();   // serviceId = user-service
        Assert.state(serviceId != null, "Request URI does not contain a valid hostname: " + originalUri);
        String hint = getHint(serviceId);
        // 把Feign的Request包装成LoadBalancer能理解的请求上下文（通过buildRequestData方法）
        DefaultRequest<RequestDataContext> lbRequest =
                new DefaultRequest<>(new RequestDataContext(LoadBalancerUtils.buildRequestData(request), hint));
        /**
         * 找到支持的LoadBalancerLifecycle。
         * 可以理解成给负载均衡过程和请求执行过程埋一些观察、统计、监控、回调等扩展点
         */
        Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
                .getSupportedLifecycleProcessors(
                        loadBalancerClientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
                        RequestDataContext.class, ResponseData.class, ServiceInstance.class);
        // 开始寻找对应服务实例，并包装成DefaultResponse
        supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
        ServiceInstance instance = loadBalancerClient.choose(serviceId, lbRequest);
        org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponse = new DefaultResponse(instance);
        // 如果没找到实例，返回503错误
        if (instance == null) {
            String message = "Load balancer does not contain an instance for the service " + serviceId;
            if (LOG.isWarnEnabled())
                LOG.warn(message);

            supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
                    .onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
                            CompletionContext.Status.DISCARD, lbRequest, lbResponse)));
            return Response.builder()
                    .request(request)
                    .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                    .body(message, StandardCharsets.UTF_8)
                    .build();
        }
        // 找到了服务实例，开始生成真实的url地址
        String reconstructedUrl = loadBalancerClient.reconstructURI(instance, originalUri).toString();
        // 构建新的feign request，buildRequest方法里面会走transformers扩展点
        Request newRequest = buildRequest(request, reconstructedUrl, instance);
        // 发起最终请求
        return LoadBalancerUtils.executeWithLoadBalancerLifecycleProcessing(delegate, options, newRequest, lbRequest, lbResponse,
                supportedLifecycleProcessors);
    }

    protected Request buildRequest(Request request, String reconstructedUrl) {
        return Request.create(request.httpMethod(), reconstructedUrl, request.headers(), request.body(),
                request.charset(), request.requestTemplate());
    }

    protected Request buildRequest(Request request, String reconstructedUrl, ServiceInstance instance) {
        Request newRequest = buildRequest(request, reconstructedUrl);
        if (transformers != null) {
            for (LoadBalancerFeignRequestTransformer transformer : transformers)
                newRequest = transformer.transformRequest(newRequest, instance);
        }
        return newRequest;
    }

    public Client getDelegate() {
        return delegate;
    }

    /**
     * hint是传给LoadBalancer的选择实例的参考信息。
     * 优先取当前serviceId对应的hint，如果没有则取default hint，再没有就取default。
     */
    private String getHint(String serviceId) {
        LoadBalancerProperties properties = loadBalancerClientFactory.getProperties(serviceId);
        String defaultHint = properties.getHint().getOrDefault("default", "default");
        String hintPropertyValue = properties.getHint().get(serviceId);
        return hintPropertyValue != null ? hintPropertyValue : defaultHint;
    }
}
