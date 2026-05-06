package com.github.liyibo1110.openfeign;

import com.github.liyibo1110.openfeign.clientconfig.FeignClientConfigurer;
import feign.Capability;
import feign.Client;
import feign.Contract;
import feign.ExceptionPropagationPolicy;
import feign.Feign;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.Request;
import feign.RequestInterceptor;
import feign.ResponseInterceptor;
import feign.Retryer;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 通过getObject方法委托Feign core的ReflectiveFeign，生成真正的Feign实例，最终注入给的用户声明的注入引用。
 * @author liyibo
 * @date 2026-05-06 10:19
 */
public class FeignClientFactoryBean implements FactoryBean<Object>, InitializingBean, ApplicationContextAware, BeanFactoryAware {
    private static final Log LOG = LogFactory.getLog(FeignClientFactoryBean.class);

    /** 原始接口类型，例如UserClient */
    private Class<?> type;

    /** 服务名称，例如user-service，如果没有配置固定的url字段，则后面会用这个name走负载均衡，name就是服务发现里面的serviceId */
    private String name;

    /** 配置了url就不会走服务发现负载均衡了，而是直接请求这个固定地址 */
    private String url;

    /** 区分不同Feign Client */
    private String contextId;

    /** 统一前缀路径，会通过cleanPath再做一次规范化 */
    private String path;

    /** 如果为true，会调用builder.dismiss404()，即遇到404时，不走ErrorDecoder抛异常，而是让Decoder继续处理响应 */
    private boolean dismiss404;

    /**
     * 控制当前Feign Client的子上下文是否继承父Spring容器里的配置Bean，
     * 如果为true：当前Feign Client可以看到父容器里的Encoder、Decoder、Interceptor等Bean。
     * 如果为false：当前Feign Client只能看到当前自己上下文的Bean，不向父容器查找。
     */
    private boolean inheritParentContext = true;

    /** 当前Feign Client是否支持配置刷新，如果开启会影响：Request.Options和RefreshableUrl */
    private boolean refreshableClient = false;

    private ApplicationContext applicationContext;

    private BeanFactory beanFactory;

    private Class<?> fallback = void.class;

    private Class<?> fallbackFactory = void.class;

    /** 读取超时时间 */
    private int readTimeoutMillis = new Request.Options().readTimeoutMillis();

    /** 连接超时时间 */
    private int connectTimeoutMillis = new Request.Options().connectTimeoutMillis();

    /** 是否跟随重定向 */
    private boolean followRedirects = new Request.Options().isFollowRedirects();

    /** 用来额外定制Feign Builder */
    private final List<FeignBuilderCustomizer> additionalCustomizers = new ArrayList<>();

    /** 在Registrar注册BeanDefinition时设置进来的，用于Spring注入时的限定名 */
    private String[] qualifiers = new String[] {};

    public FeignClientFactoryBean() {
        if (LOG.isDebugEnabled())
            LOG.debug("Creating a FeignClientFactoryBean.");
    }

    /**
     * 验证contextId和name必须有值，即：
     * 1、我是谁
     * 2、我要调用哪个服务
     */
    @Override
    public void afterPropertiesSet() {
        Assert.hasText(contextId, "Context id must be set");
        Assert.hasText(name, "Name must be set");
    }

    /**
     * 创建Builder，设置了必需组件：Logger、Encoder、Decoder、Contract。
     * 拿不到就会报错，因为上面都是必要组件。
     */
    protected Feign.Builder feign(FeignClientFactory context) {
        FeignLoggerFactory loggerFactory = get(context, FeignLoggerFactory.class);
        Logger logger = loggerFactory.create(type);

        // @formatter:off
        Feign.Builder builder = get(context, Feign.Builder.class)
                // required values
                .logger(logger)
                .encoder(get(context, Encoder.class))
                .decoder(get(context, Decoder.class))
                .contract(get(context, Contract.class));
        // @formatter:on

        configureFeign(context, builder);

        return builder;
    }

    /**
     * 在生成最终代理之前，会调用这个方法，负责做两类定制：
     * 1、从FeignClientFactory上下文里拿FeignBuilderCustomizer Bean。
     * 2、应用additionalCustomizers。
     * 是Builder创建和配置完成后的最后补充定制点。
     */
    private void applyBuildCustomizers(FeignClientFactory context, Feign.Builder builder) {
        Map<String, FeignBuilderCustomizer> customizerMap = context.getInstances(contextId, FeignBuilderCustomizer.class);

        if (customizerMap != null) {
            customizerMap.values()
                    .stream()
                    .sorted(AnnotationAwareOrderComparator.INSTANCE)
                    .forEach(feignBuilderCustomizer -> feignBuilderCustomizer.customize(builder));
        }
        additionalCustomizers.forEach(customizer -> customizer.customize(builder));
    }

    /**
     * 配置来源合并总入口。
     * 负责把Java配置类里的Bean和application.yml里的配置，合并到Feign.Builder上面。
     */
    protected void configureFeign(FeignClientFactory context, Feign.Builder builder) {
        FeignClientProperties properties = beanFactory != null
                ? beanFactory.getBean(FeignClientProperties.class)
                : applicationContext.getBean(FeignClientProperties.class);

        FeignClientConfigurer feignClientConfigurer = getOptional(context, FeignClientConfigurer.class);
        setInheritParentContext(feignClientConfigurer.inheritParentConfiguration());

        if (properties != null && inheritParentContext) {
            if (properties.isDefaultToProperties()) {
                configureUsingConfiguration(context, builder);
                configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()),
                        properties.getConfig().get(contextId), builder);
            } else {
                configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()),
                        properties.getConfig().get(contextId), builder);
                configureUsingConfiguration(context, builder);
            }
            configureDefaultRequestElements(properties.getConfig().get(properties.getDefaultConfig()),
                    properties.getConfig().get(contextId), builder);
        }
        else {
            configureUsingConfiguration(context, builder);
        }
    }

    /**
     * 从Spring Bean里面拿配置组件。
     */
    protected void configureUsingConfiguration(FeignClientFactory context, Feign.Builder builder) {
        Logger.Level level = getInheritedAwareOptional(context, Logger.Level.class);
        if (level != null)
            builder.logLevel(level);

        Retryer retryer = getInheritedAwareOptional(context, Retryer.class);
        if (retryer != null)
            builder.retryer(retryer);

        ErrorDecoder errorDecoder = getInheritedAwareOptional(context, ErrorDecoder.class);
        if (errorDecoder != null)
            builder.errorDecoder(errorDecoder);
        else {
            FeignErrorDecoderFactory errorDecoderFactory = getOptional(context, FeignErrorDecoderFactory.class);
            if (errorDecoderFactory != null) {
                ErrorDecoder factoryErrorDecoder = errorDecoderFactory.create(type);
                builder.errorDecoder(factoryErrorDecoder);
            }
        }
        Request.Options options = getInheritedAwareOptional(context, Request.Options.class);
        if (options == null)
            options = getOptionsByName(context, contextId);

        if (options != null) {
            builder.options(options);
            readTimeoutMillis = options.readTimeoutMillis();
            connectTimeoutMillis = options.connectTimeoutMillis();
            followRedirects = options.isFollowRedirects();
        }
        Map<String, RequestInterceptor> requestInterceptors = getInheritedAwareInstances(context, RequestInterceptor.class);
        if (requestInterceptors != null) {
            List<RequestInterceptor> interceptors = new ArrayList<>(requestInterceptors.values());
            AnnotationAwareOrderComparator.sort(interceptors);
            builder.requestInterceptors(interceptors);
        }
        ResponseInterceptor responseInterceptor = getInheritedAwareOptional(context, ResponseInterceptor.class);
        if (responseInterceptor != null)
            builder.responseInterceptor(responseInterceptor);

        QueryMapEncoder queryMapEncoder = getInheritedAwareOptional(context, QueryMapEncoder.class);
        if (queryMapEncoder != null)
            builder.queryMapEncoder(queryMapEncoder);

        if (dismiss404)
            builder.dismiss404();

        ExceptionPropagationPolicy exceptionPropagationPolicy = getInheritedAwareOptional(context, ExceptionPropagationPolicy.class);
        if (exceptionPropagationPolicy != null)
            builder.exceptionPropagationPolicy(exceptionPropagationPolicy);

        /**
         * Spring Cloud里面的Micrometer、Caching、Observation这类增强，最后很多都是通过Capability加到Feign core里面的。
         */
        Map<String, Capability> capabilities = getInheritedAwareInstances(context, Capability.class);
        if (capabilities != null) {
            capabilities.values()
                    .stream()
                    .sorted(AnnotationAwareOrderComparator.INSTANCE)
                    .forEach(builder::addCapability);
        }
    }

    /**
     * 从properties文件里面拿配置组件，默认配置文件优先级更高。
     */
    protected void configureUsingProperties(FeignClientProperties.FeignClientConfiguration baseConfig,
                                            FeignClientProperties.FeignClientConfiguration finalConfig,
                                            Feign.Builder builder) {
        configureUsingProperties(baseConfig, builder);
        configureUsingProperties(finalConfig, builder);
        Boolean dismiss404 = finalConfig != null && finalConfig.getDismiss404() != null ? finalConfig.getDismiss404()
                : (baseConfig != null && baseConfig.getDismiss404() != null ? baseConfig.getDismiss404() : null);
        if (dismiss404 != null) {
            if (dismiss404)
                builder.dismiss404();
        }
    }

    protected void configureUsingProperties(FeignClientProperties.FeignClientConfiguration config,
                                            Feign.Builder builder) {
        if (config == null)
            return;

        if (config.getLoggerLevel() != null)
            builder.logLevel(config.getLoggerLevel());

        if (!refreshableClient) {
            connectTimeoutMillis = config.getConnectTimeout() != null ? config.getConnectTimeout() : connectTimeoutMillis;
            readTimeoutMillis = config.getReadTimeout() != null ? config.getReadTimeout() : readTimeoutMillis;
            followRedirects = config.isFollowRedirects() != null ? config.isFollowRedirects() : followRedirects;

            builder.options(new Request.Options(connectTimeoutMillis, TimeUnit.MILLISECONDS, readTimeoutMillis,
                    TimeUnit.MILLISECONDS, followRedirects));
        }

        if (config.getRetryer() != null) {
            Retryer retryer = getOrInstantiate(config.getRetryer());
            builder.retryer(retryer);
        }

        if (config.getErrorDecoder() != null) {
            ErrorDecoder errorDecoder = getOrInstantiate(config.getErrorDecoder());
            builder.errorDecoder(errorDecoder);
        }

        if (config.getRequestInterceptors() != null && !config.getRequestInterceptors().isEmpty()) {
            // this will add request interceptor to builder, not replace existing
            for (Class<RequestInterceptor> bean : config.getRequestInterceptors()) {
                RequestInterceptor interceptor = getOrInstantiate(bean);
                builder.requestInterceptor(interceptor);
            }
        }

        if (config.getResponseInterceptor() != null)
            builder.responseInterceptor(getOrInstantiate(config.getResponseInterceptor()));

        if (Objects.nonNull(config.getEncoder()))
            builder.encoder(getOrInstantiate(config.getEncoder()));

        if (Objects.nonNull(config.getDecoder()))
            builder.decoder(getOrInstantiate(config.getDecoder()));

        if (Objects.nonNull(config.getContract()))
            builder.contract(getOrInstantiate(config.getContract()));

        if (Objects.nonNull(config.getExceptionPropagationPolicy()))
            builder.exceptionPropagationPolicy(config.getExceptionPropagationPolicy());

        if (config.getCapabilities() != null)
            config.getCapabilities().stream().map(this::getOrInstantiate).forEach(builder::addCapability);

        if (config.getQueryMapEncoder() != null)
            builder.queryMapEncoder(getOrInstantiate(config.getQueryMapEncoder()));
    }

    /**
     * 会把默认header和query转成RequestInterceptor，加入到Builder里面。
     */
    protected void configureDefaultRequestElements(FeignClientProperties.FeignClientConfiguration defaultConfig,
                                                   FeignClientProperties.FeignClientConfiguration clientConfig, Feign.Builder builder) {
        Map<String, Collection<String>> defaultRequestHeaders = new HashMap<>();
        if (defaultConfig != null)
            defaultConfig.getDefaultRequestHeaders().forEach((k, v) -> defaultRequestHeaders.put(k, new ArrayList<>(v)));

        if (clientConfig != null)
            clientConfig.getDefaultRequestHeaders().forEach((k, v) -> defaultRequestHeaders.put(k, new ArrayList<>(v)));

        if (!defaultRequestHeaders.isEmpty())
            addDefaultRequestHeaders(defaultRequestHeaders, builder);

        Map<String, Collection<String>> defaultQueryParameters = new HashMap<>();
        if (defaultConfig != null)
            defaultConfig.getDefaultQueryParameters().forEach((k, v) -> defaultQueryParameters.put(k, new ArrayList<>(v)));

        if (clientConfig != null)
            clientConfig.getDefaultQueryParameters().forEach((k, v) -> defaultQueryParameters.put(k, new ArrayList<>(v)));

        if (!defaultQueryParameters.isEmpty())
            addDefaultQueryParams(defaultQueryParameters, builder);
    }

    private void addDefaultQueryParams(Map<String, Collection<String>> defaultQueryParameters, Feign.Builder builder) {
        builder.requestInterceptor(requestTemplate -> {
            Map<String, Collection<String>> queries = requestTemplate.queries();
            defaultQueryParameters.keySet().forEach(key -> {
                if (!queries.containsKey(key))
                    requestTemplate.query(key, defaultQueryParameters.get(key));
            });
        });
    }

    private void addDefaultRequestHeaders(Map<String, Collection<String>> defaultRequestHeaders, Feign.Builder builder) {
        builder.requestInterceptor(requestTemplate -> {
            Map<String, Collection<String>> headers = requestTemplate.headers();
            defaultRequestHeaders.keySet().forEach(key -> {
                if (!headers.containsKey(key))
                    requestTemplate.header(key, defaultRequestHeaders.get(key));
            });
        });
    }

    /**
     * 优先从Spring Bean容器里面取，如果没有就直接new一个出来。
     */
    private <T> T getOrInstantiate(Class<T> tClass) {
        try {
            return beanFactory != null ? beanFactory.getBean(tClass) : applicationContext.getBean(tClass);
        } catch (NoSuchBeanDefinitionException e) {
            return BeanUtils.instantiateClass(tClass);
        }
    }

    /**
     * 根据contextId从组件库里面找对应的组件对象。
     */
    protected <T> T get(FeignClientFactory context, Class<T> type) {
        T instance = context.getInstance(contextId, type);
        if (instance == null)
            throw new IllegalStateException("No bean found of type " + type + " for " + contextId);
        return instance;
    }

    protected <T> T getOptional(FeignClientFactory context, Class<T> type) {
        return context.getInstance(contextId, type);
    }

    protected <T> T getInheritedAwareOptional(FeignClientFactory context, Class<T> type) {
        if (inheritParentContext)
            return getOptional(context, type);
        else
            return context.getInstanceWithoutAncestors(contextId, type);
    }

    protected <T> Map<String, T> getInheritedAwareInstances(FeignClientFactory context, Class<T> type) {
        if (inheritParentContext)
            return context.getInstances(contextId, type);
        else
            return context.getInstancesWithoutAncestors(contextId, type);
    }

    protected <T> T loadBalance(Feign.Builder builder, FeignClientFactory context, Target.HardCodedTarget<T> target) {
        Client client = getOptional(context, Client.class);
        if (client != null) {
            builder.client(client);
            applyBuildCustomizers(context, builder);
            Targeter targeter = get(context, Targeter.class);
            return targeter.target(this, builder, context, target);
        }

        throw new IllegalStateException("No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-loadbalancer?");
    }

    /**
     * 通过bean名称从上下文中获取Options bean。
     */
    protected Request.Options getOptionsByName(FeignClientFactory context, String contextId) {
        if (refreshableClient) {
            return context.getInstance(contextId, Request.Options.class.getCanonicalName() + "-" + contextId,
                    Request.Options.class);
        }
        return null;
    }

    @Override
    public Object getObject() {
        return getTarget();
    }

    /**
     * 核心方法：创建最终Feign代理的主流程。
     */
    @SuppressWarnings("unchecked")
    <T> T getTarget() {
        /**
         * 一、获取FeignClientFactory（为了拿各个组件）
         */
        FeignClientFactory feignClientFactory = beanFactory != null
                ? beanFactory.getBean(FeignClientFactory.class)
                : applicationContext.getBean(FeignClientFactory.class);
        /**
         * 二、创建Feign Builder组件
         */
        Feign.Builder builder = feign(feignClientFactory);

        /**
         * 三、分支判断：负载均衡 or 固定URL
         */
        if (!StringUtils.hasText(url) && !isUrlAvailableInConfig(contextId)) {
            // 走负载均衡路线
            if (LOG.isInfoEnabled())
                LOG.info("For '" + name + "' URL not provided. Will try picking an instance via load-balancing.");

            /**
             * url会先变成http://user-service这样子。
             * 真实请求时，会由FeignBlockingLoadBalancerClient或者RetryableFeignBlockingLoadBalancerClient，把user-service变成ServiceInstance
             */
            if (!name.startsWith("http://") && !name.startsWith("https://"))
                url = "http://" + name;
            else
                url = name;

            url += cleanPath();
            return (T) loadBalance(builder, feignClientFactory, new Target.HardCodedTarget<>(type, name, url));
        }
        if (StringUtils.hasText(url) && !url.startsWith("http://") && !url.startsWith("https://"))
            url = "http://" + url;  // 走url路线

        /**
         * 四、获取Client组件
         */
        Client client = getOptional(feignClientFactory, Client.class);
        if (client != null) {
            // 被LoadBalancer包装过了，需要拆开
            if (client instanceof FeignBlockingLoadBalancerClient)
                client = ((FeignBlockingLoadBalancerClient) client).getDelegate();

            // 被LoadBalancer包装过了，需要拆开
            if (client instanceof RetryableFeignBlockingLoadBalancerClient)
                client = ((RetryableFeignBlockingLoadBalancerClient) client).getDelegate();

            builder.client(client);
        }

        applyBuildCustomizers(feignClientFactory, builder);

        Targeter targeter = get(feignClientFactory, Targeter.class);
        return targeter.target(this, builder, feignClientFactory, resolveTarget(feignClientFactory, contextId, url));
    }

    /**
     * 规范化path字段
     */
    private String cleanPath() {
        if (path == null)
            return "";

        String path = this.path.trim();
        if (StringUtils.hasLength(path)) {
            if (!path.startsWith("/"))
                path = "/" + path;
            if (path.endsWith("/"))
                path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * 决定最终的Target类型，有三种目标可能：
     * 1、HardCodedTarget：对应明确给了url。
     * 2、RefreshableHardCodedTarget：开始了refreshableClient，并且Refreshable配置有值。
     * 3、PropertyBasedTarget：从配置文件spring.cloud.openfeign.client.config.xxx.url获取到的url。
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> Target.HardCodedTarget<T> resolveTarget(FeignClientFactory context, String contextId, String url) {
        if (StringUtils.hasText(url))
            return new Target.HardCodedTarget(type, name, url + cleanPath());

        if (refreshableClient) {
            RefreshableUrl refreshableUrl = context.getInstance(contextId, RefreshableUrl.class.getCanonicalName() + "-" + contextId, RefreshableUrl.class);
            if (Objects.nonNull(refreshableUrl) && StringUtils.hasText(refreshableUrl.getUrl()))
                return new RefreshableHardCodedTarget<>(type, name, refreshableUrl, cleanPath());
        }
        FeignClientProperties.FeignClientConfiguration config = findConfigByKey(contextId);
        if (Objects.isNull(config) || !StringUtils.hasText(config.getUrl()))
            throw new IllegalStateException("Provide Feign client URL either in @FeignClient() or in config properties.");

        return new PropertyBasedTarget(type, name, config, cleanPath());
    }

    private boolean isUrlAvailableInConfig(String contextId) {
        FeignClientProperties.FeignClientConfiguration config = findConfigByKey(contextId);
        return Objects.nonNull(config) && StringUtils.hasText(config.getUrl());
    }

    private FeignClientProperties.FeignClientConfiguration findConfigByKey(String configKey) {
        FeignClientProperties properties = beanFactory != null
                ? beanFactory.getBean(FeignClientProperties.class)
                : applicationContext.getBean(FeignClientProperties.class);
        return properties.getConfig().get(configKey);
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isDismiss404() {
        return dismiss404;
    }

    public void setDismiss404(boolean dismiss404) {
        this.dismiss404 = dismiss404;
    }

    public boolean isInheritParentContext() {
        return inheritParentContext;
    }

    public void setInheritParentContext(boolean inheritParentContext) {
        this.inheritParentContext = inheritParentContext;
    }

    public void addCustomizer(FeignBuilderCustomizer customizer) {
        additionalCustomizers.add(customizer);
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
        beanFactory = context;
    }

    public Class<?> getFallback() {
        return fallback;
    }

    public void setFallback(Class<?> fallback) {
        this.fallback = fallback;
    }

    public Class<?> getFallbackFactory() {
        return fallbackFactory;
    }

    public void setFallbackFactory(Class<?> fallbackFactory) {
        this.fallbackFactory = fallbackFactory;
    }

    public void setRefreshableClient(boolean refreshableClient) {
        this.refreshableClient = refreshableClient;
    }

    public String[] getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(String[] qualifiers) {
        this.qualifiers = qualifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        FeignClientFactoryBean that = (FeignClientFactoryBean) o;
        return Objects.equals(applicationContext, that.applicationContext)
                && Objects.equals(beanFactory, that.beanFactory) && dismiss404 == that.dismiss404
                && inheritParentContext == that.inheritParentContext && Objects.equals(fallback, that.fallback)
                && Objects.equals(fallbackFactory, that.fallbackFactory) && Objects.equals(name, that.name)
                && Objects.equals(path, that.path) && Objects.equals(type, that.type) && Objects.equals(url, that.url)
                && Objects.equals(connectTimeoutMillis, that.connectTimeoutMillis)
                && Objects.equals(readTimeoutMillis, that.readTimeoutMillis)
                && Objects.equals(followRedirects, that.followRedirects)
                && Objects.equals(refreshableClient, that.refreshableClient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationContext, beanFactory, dismiss404, inheritParentContext, fallback,
                fallbackFactory, name, path, type, url, readTimeoutMillis, connectTimeoutMillis, followRedirects,
                refreshableClient);
    }

    @Override
    public String toString() {
        return new StringBuilder("FeignClientFactoryBean{").append("type=")
                .append(type)
                .append(", ")
                .append("name='")
                .append(name)
                .append("', ")
                .append("url='")
                .append(url)
                .append("', ")
                .append("path='")
                .append(path)
                .append("', ")
                .append("dismiss404=")
                .append(dismiss404)
                .append(", ")
                .append("inheritParentContext=")
                .append(inheritParentContext)
                .append(", ")
                .append("applicationContext=")
                .append(applicationContext)
                .append(", ")
                .append("beanFactory=")
                .append(beanFactory)
                .append(", ")
                .append("fallback=")
                .append(fallback)
                .append(", ")
                .append("fallbackFactory=")
                .append(fallbackFactory)
                .append("}")
                .append("connectTimeoutMillis=")
                .append(connectTimeoutMillis)
                .append("}")
                .append("readTimeoutMillis=")
                .append(readTimeoutMillis)
                .append("}")
                .append("followRedirects=")
                .append(followRedirects)
                .append("refreshableClient=")
                .append(refreshableClient)
                .append("}")
                .toString();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
