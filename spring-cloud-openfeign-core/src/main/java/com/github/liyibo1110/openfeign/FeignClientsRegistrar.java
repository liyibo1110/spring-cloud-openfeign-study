package com.github.liyibo1110.openfeign;

import feign.Request;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 在Spring启动解析@EnableFeignClients注解时，把每一个@FeignClient注解标记的接口，注册成Spring BeanDefinition，并顺便注册每个客户端自己的配置描述信息。
 *
 * 3个接口：
 * 1、ImportBeanDefinitionRegistrar：可以在@Import阶段向Spring容器注册BeanDefinition。
 * 2、ResourceLoaderAware：可以获取资源加载器，后面用于classpath扫描。
 * 3、EnvironmentAware：可以拿到Environment，后面用于解析${...}配置占位符。
 * @author liyibo
 * @date 2026-05-03 13:40
 */
public class FeignClientsRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private ResourceLoader resourceLoader;

    private Environment environment;

    FeignClientsRegistrar() {}

    /**
     * 检查Fallback组件不能是接口。
     */
    static void validateFallback(final Class clazz) {
        Assert.isTrue(!clazz.isInterface(), "Fallback class must implement the interface annotated by @FeignClient");
    }

    /**
     * 检查FallbackFactory组件不能是接口。
     */
    static void validateFallbackFactory(final Class clazz) {
        Assert.isTrue(!clazz.isInterface(), "Fallback factory must produce instances of fallback classes that implement the interface annotated by @FeignClient");
    }

    static String getName(String name) {
        if (!StringUtils.hasText(name))
            return "";

        String host = null;
        try {
            String url;
            if (!name.startsWith("http://") && !name.startsWith("https://"))
                url = "http://" + name;
            else
                url = name;
            host = new URI(url).getHost();
        }
        catch (URISyntaxException ignored) {

        }
        Assert.state(host != null, "Service id not legal hostname (" + name + ")");
        return name;
    }

    static String getUrl(String url) {
        if (StringUtils.hasText(url) && !(url.startsWith("#{") && url.contains("}"))) {
            if (!url.contains("://"))
                url = "http://" + url;

            if (url.endsWith("/"))
                url = url.substring(0, url.length() - 1);

            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(url + " is malformed", e);
            }
        }
        return url;
    }

    static String getPath(String path) {
        if (StringUtils.hasText(path)) {
            path = path.trim();
            if (!path.startsWith("/"))
                path = "/" + path;

            if (path.endsWith("/"))
                path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        // 注册@EnableFeignClients(defaultConfiguration = Xxx.class)，即@EnableFeignClients本身的默认配置
        registerDefaultConfiguration(metadata, registry);
        // 扫描并注册所有@FeignClient接口，即每个@FeignClient自己的接口定义
        registerFeignClients(metadata, registry);
    }

    /**
     * 注册@EnableFeignClients(defaultConfiguration = Xxx.class)，即@EnableFeignClients本身的默认配置
     */
    private void registerDefaultConfiguration(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        // 获取EnableFeignClients注解里面的所有属性值
        Map<String, Object> defaultAttrs = metadata.getAnnotationAttributes(EnableFeignClients.class.getName(), true);

        if (defaultAttrs != null && defaultAttrs.containsKey("defaultConfiguration")) {
            String name;
            if (metadata.hasEnclosingClass())
                name = "default." + metadata.getEnclosingClassName();
            else
                name = "default." + metadata.getClassName();

            registerClientConfiguration(registry, name, "default", defaultAttrs.get("defaultConfiguration"));
        }
    }

    /**
     * 扫描并注册所有@FeignClient接口，即每个@FeignClient自己的接口定义。
     *
     * 每个@FeignClient最终会注册两类东西：
     * 1、FeignClientSpecification：即保存这个client的configuration配置。
     * 2、Feign Client本体的BeanDefinition：后续用于创建代理对象。
     */
    public void registerFeignClients(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        LinkedHashSet<BeanDefinition> candidateComponents = new LinkedHashSet<>();
        // 先拿@EnableFeignClients上的属性，然后分成两种模式
        Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableFeignClients.class.getName());
        final Class<?>[] clients = attrs == null ? null : (Class<?>[]) attrs.get("clients");
        // 没有显式指定clients字段，就走包扫描
        if (clients == null || clients.length == 0) {
            // 创建scanner
            ClassPathScanningCandidateComponentProvider scanner = getScanner();
            // 设置扫描资源加载器
            scanner.setResourceLoader(this.resourceLoader);
            // 只扫描带@FeignClient的类型
            scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));
            // 计算basePackages
            Set<String> basePackages = getBasePackages(metadata);
            // 找到所有的候选BeanDefinition
            for (String basePackage : basePackages)
                candidateComponents.addAll(scanner.findCandidateComponents(basePackage));
        } else {
            // 如果显式指定了clients，则不扫描，并直接注册
            for (Class<?> clazz : clients)
                candidateComponents.add(new AnnotatedGenericBeanDefinition(clazz));
        }

        // 遍历上面扫描到的候选组件
        for (BeanDefinition candidateComponent : candidateComponents) {
            if (candidateComponent instanceof AnnotatedBeanDefinition beanDefinition) {
                // verify annotated class is an interface
                AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                // 1、校验@FeignClient只能标在接口上
                Assert.isTrue(annotationMetadata.isInterface(), "@FeignClient can only be specified on an interface");
                // 2、读取@FeignClient注解属性
                Map<String, Object> attributes = annotationMetadata.getAnnotationAttributes(FeignClient.class.getCanonicalName());

                String name = getClientName(attributes);
                String className = annotationMetadata.getClassName();
                // 3、注册这个client专属的FeignClientSpecification
                registerClientConfiguration(registry, name, className, attributes.get("configuration"));
                // 4、注册真正代表Feign接口的BeanDefinition
                registerFeignClient(registry, annotationMetadata, attributes);
            }
        }
    }

    /**
     * 根据属性解析策略，选择是eager还是lazy。
     */
    private void registerFeignClient(BeanDefinitionRegistry registry, AnnotationMetadata annotationMetadata,
                                     Map<String, Object> attributes) {
        String className = annotationMetadata.getClassName();
        /**
         * spring.cloud.openfeign.lazy-attributes-resolution，默认值是false，所有优先走eager，
         * 即启动注册阶段就尽量解析@FeignClient的name、url、path、contextId等属性
         */
        if (String.valueOf(false).equals(environment.getProperty("spring.cloud.openfeign.lazy-attributes-resolution",
                String.valueOf(false)))) {
            eagerlyRegisterFeignClientBeanDefinition(className, attributes, registry);
        } else {
            // 很多属性推迟到Bean真正创建时再解析
            lazilyRegisterFeignClientBeanDefinition(className, attributes, registry);
        }
    }

    /**
     * 默认主线
     */
    private void eagerlyRegisterFeignClientBeanDefinition(String className, Map<String, Object> attributes,
                                                          BeanDefinitionRegistry registry) {
        validate(attributes);
        /**
         * 核心是注册一个FeignClientFactoryBean（注意是FactoryBean，不是Feign代理本身）。
         * 本质就是在构造FeignClientFactoryBean的BeanDefinition，它后面会负责getObject()，即创建真正的Feign代理对象，举例：
         * Spring Bean 名称：com.xxx.UserClient
         * BeanClass：FeignClientFactoryBean
         * FactoryBean objectType：com.xxx.UserClient
         * 最终getBean(UserClient.class) 得到：Feign动态代理对象
         */
        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(FeignClientFactoryBean.class);
        definition.addPropertyValue("url", getUrl(null, attributes));
        definition.addPropertyValue("path", getPath(null, attributes));
        String name = getName(attributes);
        definition.addPropertyValue("name", name);
        String contextId = getContextId(null, attributes);
        definition.addPropertyValue("contextId", contextId);
        definition.addPropertyValue("type", className);
        definition.addPropertyValue("dismiss404", Boolean.parseBoolean(String.valueOf(attributes.get("dismiss404"))));
        Object fallback = attributes.get("fallback");
        if (fallback != null) {
            definition.addPropertyValue("fallback",
                    (fallback instanceof Class ? fallback : ClassUtils.resolveClassName(fallback.toString(), null)));
        }
        Object fallbackFactory = attributes.get("fallbackFactory");
        if (fallbackFactory != null) {
            definition.addPropertyValue("fallbackFactory", fallbackFactory instanceof Class ? fallbackFactory
                    : ClassUtils.resolveClassName(fallbackFactory.toString(), null));
        }
        definition.addPropertyValue("fallbackFactory", attributes.get("fallbackFactory"));
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        definition.addPropertyValue("refreshableClient", isClientRefreshEnabled());
        String[] qualifiers = getQualifiers(attributes);
        if (ObjectUtils.isEmpty(qualifiers))
            qualifiers = new String[] { contextId + "FeignClient" };

        // This is done so that there's a way to retrieve qualifiers while generating AOT
        // code
        definition.addPropertyValue("qualifiers", qualifiers);
        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
        Class<?> type = ClassUtils.resolveClassName(className, null);
        /**
         * 重点：它告诉Spring：
         * 虽然这个BeanDefinition的beanClass是FeignClientFactoryBean，
         * 但是它最终生产出来的对象类型是UserClient接口。
         */
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, type);
        // has a default, won't be null
        boolean primary = (Boolean) attributes.get("primary");
        beanDefinition.setPrimary(primary);
        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, qualifiers);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
        registerRefreshableBeanDefinition(registry, contextId, Request.Options.class, OptionsFactoryBean.class);
        registerRefreshableBeanDefinition(registry, contextId, RefreshableUrl.class, RefreshableUrlFactoryBean.class);
    }

    /**
     * 非默认的延迟策略分支
     */
    private void lazilyRegisterFeignClientBeanDefinition(String className, Map<String, Object> attributes,
                                                         BeanDefinitionRegistry registry) {
        ConfigurableBeanFactory beanFactory = registry instanceof ConfigurableBeanFactory
                ? (ConfigurableBeanFactory) registry : null;
        Class clazz = ClassUtils.resolveClassName(className, null);
        String contextId = getContextId(beanFactory, attributes);
        String name = getName(attributes);
        FeignClientFactoryBean factoryBean = new FeignClientFactoryBean();
        factoryBean.setBeanFactory(beanFactory);
        factoryBean.setName(name);
        factoryBean.setContextId(contextId);
        factoryBean.setType(clazz);
        factoryBean.setRefreshableClient(isClientRefreshEnabled());
        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(clazz, () -> {
            factoryBean.setUrl(getUrl(beanFactory, attributes));
            factoryBean.setPath(getPath(beanFactory, attributes));
            factoryBean.setDismiss404(Boolean.parseBoolean(String.valueOf(attributes.get("dismiss404"))));
            Object fallback = attributes.get("fallback");
            if (fallback != null) {
                factoryBean.setFallback(fallback instanceof Class ? (Class<?>) fallback
                        : ClassUtils.resolveClassName(fallback.toString(), null));
            }
            Object fallbackFactory = attributes.get("fallbackFactory");
            if (fallbackFactory != null) {
                factoryBean.setFallbackFactory(fallbackFactory instanceof Class ? (Class<?>) fallbackFactory
                        : ClassUtils.resolveClassName(fallbackFactory.toString(), null));
            }
            return factoryBean.getObject();
        });
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        definition.setLazyInit(true);
        validate(attributes);

        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute("feignClientsRegistrarFactoryBean", factoryBean);

        // has a default, won't be null
        boolean primary = (Boolean) attributes.get("primary");

        beanDefinition.setPrimary(primary);

        String[] qualifiers = getQualifiers(attributes);
        if (ObjectUtils.isEmpty(qualifiers)) {
            qualifiers = new String[] { contextId + "FeignClient" };
        }

        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, qualifiers);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);

        registerRefreshableBeanDefinition(registry, contextId, Request.Options.class, OptionsFactoryBean.class);
        registerRefreshableBeanDefinition(registry, contextId, RefreshableUrl.class, RefreshableUrlFactoryBean.class);
    }

    private void validate(Map<String, Object> attributes) {
        AnnotationAttributes annotation = AnnotationAttributes.fromMap(attributes);
        // This blows up if an aliased property is overspecified
        // FIXME annotation.getAliasedString("name", FeignClient.class, null);
        validateFallback(annotation.getClass("fallback"));
        validateFallbackFactory(annotation.getClass("fallbackFactory"));
    }

    String getName(Map<String, Object> attributes) {
        String name = (String) attributes.get("serviceId");
        if (!StringUtils.hasText(name))
            name = (String) attributes.get("name");

        if (!StringUtils.hasText(name))
            name = (String) attributes.get("value");

        name = resolve(null, name);
        return getName(name);
    }

    private String getContextId(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
        String contextId = (String) attributes.get("contextId");
        if (!StringUtils.hasText(contextId))
            return getName(attributes);

        contextId = resolve(beanFactory, contextId);
        return getName(contextId);
    }

    private String resolve(ConfigurableBeanFactory beanFactory, String value) {
        if (StringUtils.hasText(value)) {
            if (beanFactory == null)
                return this.environment.resolvePlaceholders(value);

            BeanExpressionResolver resolver = beanFactory.getBeanExpressionResolver();
            String resolved = beanFactory.resolveEmbeddedValue(value);
            if (resolver == null)
                return resolved;

            Object evaluateValue = resolver.evaluate(resolved, new BeanExpressionContext(beanFactory, null));
            if (evaluateValue != null)
                return String.valueOf(evaluateValue);

            return null;
        }
        return value;
    }

    private String getUrl(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
        String url = resolve(beanFactory, (String) attributes.get("url"));
        return getUrl(url);
    }

    private String getPath(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
        String path = resolve(beanFactory, (String) attributes.get("path"));
        return getPath(path);
    }

    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation())
                        isCandidate = true;
                }
                return isCandidate;
            }
        };
    }

    protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableFeignClients.class.getCanonicalName());

        Set<String> basePackages = new HashSet<>();
        for (String pkg : (String[]) attributes.get("value")) {
            if (StringUtils.hasText(pkg))
                basePackages.add(pkg);
        }
        for (String pkg : (String[]) attributes.get("basePackages")) {
            if (StringUtils.hasText(pkg))
                basePackages.add(pkg);
        }
        for (Class<?> clazz : (Class[]) attributes.get("basePackageClasses"))
            basePackages.add(ClassUtils.getPackageName(clazz));

        if (basePackages.isEmpty())
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));

        return basePackages;
    }

    private String getQualifier(Map<String, Object> client) {
        if (client == null)
            return null;

        String qualifier = (String) client.get("qualifier");
        if (StringUtils.hasText(qualifier))
            return qualifier;

        return null;
    }

    private String[] getQualifiers(Map<String, Object> client) {
        if (client == null)
            return null;

        List<String> qualifierList = new ArrayList<>(Arrays.asList((String[]) client.get("qualifiers")));
        qualifierList.removeIf(qualifier -> !StringUtils.hasText(qualifier));
        if (qualifierList.isEmpty() && getQualifier(client) != null)
            qualifierList = Collections.singletonList(getQualifier(client));

        return !qualifierList.isEmpty() ? qualifierList.toArray(new String[0]) : null;
    }

    private String getClientName(Map<String, Object> client) {
        if (client == null)
            return null;

        String value = (String) client.get("contextId");
        if (!StringUtils.hasText(value))
            value = (String) client.get("value");

        if (!StringUtils.hasText(value))
            value = (String) client.get("name");

        if (!StringUtils.hasText(value))
            value = (String) client.get("serviceId");

        if (StringUtils.hasText(value))
            return value;

        throw new IllegalStateException("Either 'name' or 'value' must be provided in @" + FeignClient.class.getSimpleName());
    }

    /**
     * 注册FeignClientSpecification，例如：
     * @FeignClient(name = "user-service", configuration = UserFeignConfig.class)
     * public interface UserClient {}
     *
     * BeanName:
     * user-service.FeignClientSpecification
     *
     * BeanClass:
     * FeignClientSpecification
     *
     * 构造参数:
     * name = user-service
     * className = com.xxx.UserClient
     * configuration = UserFeignConfig.class
     *
     * 会在FeignClientFactory / NamedContextFactory根据name或contextId给每个FeignClient构建自己的小型配置上下文，举例：
     * @FeignClient(name = "a", configuration = AConfig.class)
     * @FeignClient(name = "b", configuration = BConfig.class)
     * 不同的Feign Client可以有不同的Encoder、Decoder、Contract、Interceptor、Options等配置。
     */
    private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name, Object className,
                                             Object configuration) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FeignClientSpecification.class);
        builder.addConstructorArgValue(name);
        builder.addConstructorArgValue(className);
        builder.addConstructorArgValue(configuration);
        registry.registerBeanDefinition(name + "." + FeignClientSpecification.class.getSimpleName(),
                builder.getBeanDefinition());
    }

    private void registerRefreshableBeanDefinition(BeanDefinitionRegistry registry, String contextId, Class<?> beanType,
                                                   Class<?> factoryBeanType) {
        if (isClientRefreshEnabled()) {
            String beanName = beanType.getCanonicalName() + "-" + contextId;
            BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(factoryBeanType);
            definitionBuilder.setScope("refresh");
            definitionBuilder.addPropertyValue("contextId", contextId);
            BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(definitionBuilder.getBeanDefinition(), beanName);
            definitionHolder = ScopedProxyUtils.createScopedProxy(definitionHolder, registry, true);
            BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
        }
    }

    private boolean isClientRefreshEnabled() {
        return environment.getProperty("spring.cloud.openfeign.client.refresh-enabled", Boolean.class, false);
    }
}
