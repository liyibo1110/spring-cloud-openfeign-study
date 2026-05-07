package com.github.liyibo1110.openfeign.support;

import com.github.liyibo1110.openfeign.AnnotatedParameterProcessor;
import com.github.liyibo1110.openfeign.CollectionFormat;
import com.github.liyibo1110.openfeign.FeignClientProperties;
import com.github.liyibo1110.openfeign.SpringQueryMap;
import com.github.liyibo1110.openfeign.encoding.HttpEncoding;
import feign.Contract;
import feign.Feign;
import feign.MethodMetadata;
import feign.Param;
import feign.QueryMap;
import feign.Request;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Pageable;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

/**
 * 负责把接口上的Spring MVC注解，解析成Feign能理解的MethodMetadata。
 * @author liyibo
 * @date 2026-05-06 14:47
 */
public class SpringMvcContract extends Contract.BaseContract implements ResourceLoaderAware {

    private static final Log LOG = LogFactory.getLog(SpringMvcContract.class);

    private static final String ACCEPT = "Accept";

    private static final String CONTENT_TYPE = "Content-Type";

    private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);

    private static final TypeDescriptor ITERABLE_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Iterable.class);

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    /**
     * 某个参数的注解类型 -> 对应的参数解析器，默认支持以下注解：
     * @MatrixVariable
     * @PathVariable
     * @RequestParam
     * @RequestHeader
     * @SpringQueryMap / @QueryMap
     * @RequestPart
     * @CookieValue
     */
    private final Map<Class<? extends Annotation>, AnnotatedParameterProcessor> annotatedArgumentProcessors;

    /**
     * 已处理的方法缓存，即MethodMetadata -> Java Method的反查表。
     */
    private final Map<String, Method> processedMethods = new HashMap<>();

    /**
     * 用于把参数值，转换成字符串，例如：
     * @GetMapping("/users")
     * List<User> list(@RequestParam LocalDate date);
     * 最终要传给server的日期格式为?date=2026-05-06，所以要把LocalDate转换成String（还得包装成Feign的Expander）
     */
    private final ConversionService conversionService;
    private final ConvertingExpanderFactory convertingExpanderFactory;

    /**
     * 用于解析占位符，例如：
     * @GetMapping("${api.user.path}")
     * 即Spring环境变量里的占位符
     */
    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    /**
     * 路径处理开关，来自FeignClientProperties的同名字段
     */
    private final boolean decodeSlash;

    private final boolean removeTrailingSlash;

    public SpringMvcContract() {
        this(Collections.emptyList());
    }

    public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors) {
        this(annotatedParameterProcessors, new DefaultConversionService());
    }

    public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
                             ConversionService conversionService) {
        this(annotatedParameterProcessors, conversionService, true);
    }

    @Deprecated(forRemoval = true)
    public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
                             ConversionService conversionService, boolean decodeSlash) {
        this(annotatedParameterProcessors, conversionService, decodeSlash, false);
    }

    @Deprecated(forRemoval = true)
    public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
                             ConversionService conversionService, boolean decodeSlash, boolean removeTrailingSlash) {
        Assert.notNull(annotatedParameterProcessors, "Parameter processors can not be null.");
        Assert.notNull(conversionService, "ConversionService can not be null.");

        List<AnnotatedParameterProcessor> processors = getDefaultAnnotatedArgumentsProcessors();
        // 用户可以传入额外的参数处理器，让SpringMvcContract支持新的参数注解
        processors.addAll(annotatedParameterProcessors);

        annotatedArgumentProcessors = toAnnotatedArgumentProcessorMap(processors);
        this.conversionService = conversionService;
        convertingExpanderFactory = new ConvertingExpanderFactory(conversionService);
        this.decodeSlash = decodeSlash;
        this.removeTrailingSlash = removeTrailingSlash;
    }

    public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
                             ConversionService conversionService, FeignClientProperties feignClientProperties) {
        this(annotatedParameterProcessors, conversionService,
                feignClientProperties == null || feignClientProperties.isDecodeSlash(),
                feignClientProperties != null && feignClientProperties.isRemoveTrailingSlash());
    }

    private static TypeDescriptor createTypeDescriptor(Method method, int paramIndex) {
        Parameter parameter = method.getParameters()[paramIndex];
        MethodParameter methodParameter = MethodParameter.forParameter(parameter);
        TypeDescriptor typeDescriptor = new TypeDescriptor(methodParameter);

        // Feign applies the Param.Expander to each element of an Iterable, so in those
        // cases we need to provide a TypeDescriptor of the element.
        if (typeDescriptor.isAssignableTo(ITERABLE_TYPE_DESCRIPTOR)) {
            TypeDescriptor elementTypeDescriptor = getElementTypeDescriptor(typeDescriptor);

            checkState(elementTypeDescriptor != null,
                    "Could not resolve element type of Iterable type %s. Not declared?", typeDescriptor);

            typeDescriptor = elementTypeDescriptor;
        }
        return typeDescriptor;
    }

    private static TypeDescriptor getElementTypeDescriptor(TypeDescriptor typeDescriptor) {
        TypeDescriptor elementTypeDescriptor = typeDescriptor.getElementTypeDescriptor();
        // that means it's not a collection, but it is iterable, gh-135
        if (elementTypeDescriptor == null && Iterable.class.isAssignableFrom(typeDescriptor.getType())) {
            ResolvableType type = typeDescriptor.getResolvableType().as(Iterable.class).getGeneric(0);
            if (type.resolve() == null)
                return null;

            return new TypeDescriptor(type, null, typeDescriptor.getAnnotations());
        }
        return elementTypeDescriptor;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 处理接口类上面的注解。
     */
    @Override
    protected void processAnnotationOnClass(MethodMetadata data, Class<?> clz) {
        RequestMapping classAnnotation = findMergedAnnotation(clz, RequestMapping.class);
        // 不允许在@FeignClient接口上标记@RequestMapping，正确方式应该是使用path字段
        if (classAnnotation != null) {
            LOG.error("Cannot process class: " + clz.getName() + ". @RequestMapping annotation is not allowed on @FeignClient interfaces.");
            throw new IllegalArgumentException("@RequestMapping annotation not allowed on @FeignClient interfaces");
        }
        CollectionFormat collectionFormat = findMergedAnnotation(clz, CollectionFormat.class);
        if (collectionFormat != null)
            data.template().collectionFormat(collectionFormat.value());
    }

    @Override
    public MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
        processedMethods.put(Feign.configKey(targetType, method), method);
        return super.parseAndValidateMetadata(targetType, method);
    }

    /**
     * 处理方法上面的注解。
     */
    @Override
    protected void processAnnotationOnMethod(MethodMetadata data, Annotation methodAnnotation, Method method) {
        if (methodAnnotation instanceof CollectionFormat) {
            CollectionFormat collectionFormat = findMergedAnnotation(method, CollectionFormat.class);
            data.template().collectionFormat(collectionFormat.value());
        }

        /**
         * 只能处理@RequestMapping或者被@RequestMapping元注解标注的注解，即：
         * @RequestMapping
         * @GetMapping
         * @PostMapping
         * @PutMapping
         * @DeleteMapping
         * @PatchMapping
         */
        if (!(methodAnnotation instanceof RequestMapping) && !methodAnnotation.annotationType().isAnnotationPresent(RequestMapping.class))
            return;

        // 开始解析Method上面的注解
        RequestMapping methodMapping = findMergedAnnotation(method, RequestMapping.class);
        // HTTP Method
        RequestMethod[] methods = methodMapping.method();
        if (methods.length == 0)    // 没有method字段，默认用GET
            methods = new RequestMethod[] { RequestMethod.GET };

        checkOne(method, methods, "method");
        data.template().method(Request.HttpMethod.valueOf(methods[0].name()));

        // path
        checkAtMostOne(method, methodMapping.value(), "value");
        if (methodMapping.value().length > 0) {
            String pathValue = emptyToNull(methodMapping.value()[0]);
            if (pathValue != null) {
                pathValue = resolve(pathValue);
                // Append path from @RequestMapping if value is present on method
                if (!pathValue.startsWith("/") && !data.template().path().endsWith("/"))
                    pathValue = "/" + pathValue;

                if (removeTrailingSlash && pathValue.endsWith("/"))
                    pathValue = pathValue.substring(0, pathValue.length() - 1);

                data.template().uri(pathValue, true);
                if (data.template().decodeSlash() != decodeSlash)
                    data.template().decodeSlash(decodeSlash);
            }
        }

        // produces
        parseProduces(data, methodMapping);

        // consumes
        parseConsumes(data, methodMapping);

        // headers
        parseHeaders(data, methodMapping);

        // params
        parseParams(data, methodMapping);

        data.indexToExpander(new LinkedHashMap<>());
    }

    private String resolve(String value) {
        if (StringUtils.hasText(value) && resourceLoader instanceof ConfigurableApplicationContext)
            return ((ConfigurableApplicationContext) resourceLoader).getEnvironment().resolvePlaceholders(value);

        return value;
    }

    /**
     * 只允许特定注解里面有0或1个值。
     */
    private void checkAtMostOne(Method method, Object[] values, String fieldName) {
        checkState(values != null && (values.length == 0 || values.length == 1),
                "Method %s can only contain at most 1 %s field. Found: %s", method.getName(), fieldName,
                values == null ? null : Arrays.asList(values));
    }

    /**
     * 只允许特定注解里面有且仅有1个值。
     */
    private void checkOne(Method method, Object[] values, String fieldName) {
        checkState(values != null && values.length == 1, "Method %s can only contain 1 %s field. Found: %s",
                method.getName(), fieldName, values == null ? null : Arrays.asList(values));
    }

    /**
     * 处理方法特定参数上面的注解，例如：
     * @PathVariable
     * @RequestParam
     * @RequestHeader
     * @RequestPart
     * @SpringQueryMap
     * @CookieValue
     */
    @Override
    protected boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] annotations, int paramIndex) {
        boolean isHttpAnnotation = false;

        // 处理特殊的Pageable（Spring Data）
        try {
            if (Pageable.class.isAssignableFrom(data.method().getParameterTypes()[paramIndex])) {
                // do not set a Pageable as QueryMap if there's an actual QueryMap param
                // present
                if (!queryMapParamPresent(data)) {
                    data.queryMapIndex(paramIndex);
                    return false;
                }
            }
        } catch (NoClassDefFoundError ignored) {
            // Do nothing; added to avoid exceptions if optional dependency not present
        }

        AnnotatedParameterProcessor.AnnotatedParameterContext context = new SimpleAnnotatedParameterContext(data, paramIndex);
        Method method = processedMethods.get(data.configKey());
        // 查找对应的参数处理器，例如@PathVariable会找到PathVariableParameterProcessor，会修改MethodMetadata里面的RequestTemplate
        for (Annotation parameterAnnotation : annotations) {
            AnnotatedParameterProcessor processor = annotatedArgumentProcessors.get(parameterAnnotation.annotationType());
            if (processor != null) {
                Annotation processParameterAnnotation;
                // 注解上没显式提供value时，用Java参数名来作为兜底，即UserDTO getUser(@PathVariable Long id); 会提取id作为注解的value
                processParameterAnnotation = synthesizeWithMethodParameterNameAsFallbackValue(parameterAnnotation, method, paramIndex);
                isHttpAnnotation |= processor.processArgument(context, processParameterAnnotation, method);
            }
        }

        if (!isMultipartFormData(data) && isHttpAnnotation && data.indexToExpander().get(paramIndex) == null) {
            TypeDescriptor typeDescriptor = createTypeDescriptor(method, paramIndex);
            if (conversionService.canConvert(typeDescriptor, STRING_TYPE_DESCRIPTOR)) {
                Param.Expander expander = convertingExpanderFactory.getExpander(typeDescriptor);
                if (expander != null)
                    // 哪个参数下标 -> 用哪个Param.Expander转成字符串
                    data.indexToExpander().put(paramIndex, expander);
            }
        }
        return isHttpAnnotation;
    }

    private boolean queryMapParamPresent(MethodMetadata data) {
        Annotation[][] paramsAnnotations = data.method().getParameterAnnotations();
        for (int i = 0; i < paramsAnnotations.length; i++) {
            Annotation[] paramAnnotations = paramsAnnotations[i];
            Class<?> parameterType = data.method().getParameterTypes()[i];
            if (Arrays.stream(paramAnnotations).anyMatch(annotation -> Map.class.isAssignableFrom(parameterType) && annotation instanceof RequestParam
                            || annotation instanceof SpringQueryMap || annotation instanceof QueryMap)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析produces字段，注意这个对应Feign Client来说，它要表达的意思其实是：客户端的Accept。
     */
    private void parseProduces(MethodMetadata md, RequestMapping annotation) {
        String[] serverProduces = annotation.produces();
        String clientAccepts = serverProduces.length == 0 ? null : emptyToNull(serverProduces[0]);
        if (clientAccepts != null)
            md.template().header(ACCEPT, clientAccepts);
    }

    /**
     * 解析consumes字段，注意这个对应Feign Client来说，它要表达的意思其实是：客户端的Content-Type。
     */
    private void parseConsumes(MethodMetadata md, RequestMapping annotation) {
        String[] serverConsumes = annotation.consumes();
        String clientProduces = serverConsumes.length == 0 ? null : emptyToNull(serverConsumes[0]);
        if (clientProduces != null)
            md.template().header(CONTENT_TYPE, clientProduces);
    }

    /**
     * 解析headers字段，例如：
     * @GetMapping(value = "/users", headers = "X-App=demo")
     * 但是不支持!=这种不等于符号。
     */
    private void parseHeaders(MethodMetadata md, RequestMapping annotation) {
        // TODO: only supports one header value per key
        if (annotation.headers() != null) {
            for (String header : annotation.headers()) {
                int index = header.indexOf('=');
                if (!header.contains("!=") && index >= 0)
                    md.template().header(resolve(header.substring(0, index)), resolve(header.substring(index + 1).trim()));
            }
        }
    }

    /**
     * 解析params字段，例如：
     * @GetMapping(value = "/users", params = "status=1")
     */
    private void parseParams(MethodMetadata data, RequestMapping methodMapping) {
        String[] params = methodMapping.params();
        if (params == null)
            return;

        for (String param : params) {
            NameValueResolver nameValueResolver = new NameValueResolver(param);
            if (!nameValueResolver.isNegated()) {
                data.template().query(resolve(nameValueResolver.getName()), resolve(nameValueResolver.getValue()));
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("Negated params are not supported by Feign and ignored during parameter processing: " + param);
            }
        }
    }

    private Map<Class<? extends Annotation>, AnnotatedParameterProcessor> toAnnotatedArgumentProcessorMap(
            List<AnnotatedParameterProcessor> processors) {
        Map<Class<? extends Annotation>, AnnotatedParameterProcessor> result = new HashMap<>();
        for (AnnotatedParameterProcessor processor : processors)
            result.put(processor.getAnnotationType(), processor);

        return result;
    }

    private List<AnnotatedParameterProcessor> getDefaultAnnotatedArgumentsProcessors() {
        List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();

        annotatedArgumentResolvers.add(new MatrixVariableParameterProcessor());
        annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
        annotatedArgumentResolvers.add(new RequestParamParameterProcessor());
        annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());
        annotatedArgumentResolvers.add(new QueryMapParameterProcessor());
        annotatedArgumentResolvers.add(new RequestPartParameterProcessor());
        annotatedArgumentResolvers.add(new CookieValueParameterProcessor());

        return annotatedArgumentResolvers;
    }

    private Annotation synthesizeWithMethodParameterNameAsFallbackValue(Annotation parameterAnnotation, Method method,
                                                                        int parameterIndex) {
        Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(parameterAnnotation);
        Object defaultValue = AnnotationUtils.getDefaultValue(parameterAnnotation);
        if (defaultValue instanceof String && defaultValue.equals(annotationAttributes.get(AnnotationUtils.VALUE))) {
            Type[] parameterTypes = method.getGenericParameterTypes();
            String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
            if (shouldAddParameterName(parameterIndex, parameterTypes, parameterNames))
                annotationAttributes.put(AnnotationUtils.VALUE, parameterNames[parameterIndex]);
        }
        return AnnotationUtils.synthesizeAnnotation(annotationAttributes, parameterAnnotation.annotationType(), null);
    }

    private boolean shouldAddParameterName(int parameterIndex, Type[] parameterTypes, String[] parameterNames) {
        // has a parameter name
        return parameterNames != null && parameterNames.length > parameterIndex
                // has a type
                && parameterTypes != null && parameterTypes.length > parameterIndex;
    }

    private boolean isMultipartFormData(MethodMetadata data) {
        Collection<String> contentTypes = data.template().headers().get(HttpEncoding.CONTENT_TYPE);

        if (contentTypes != null && !contentTypes.isEmpty()) {
            String type = contentTypes.iterator().next();
            try {
                return Objects.equals(MediaType.valueOf(type), MediaType.MULTIPART_FORM_DATA);
            } catch (InvalidMediaTypeException ignored) {
                return false;
            }
        }

        return false;
    }

    /**
     * Spring类型转换器 -> Feign参数展开器
     */
    private record ConvertingExpanderFactory(ConversionService conversionService) {
        Param.Expander getExpander(TypeDescriptor typeDescriptor) {
            return value -> {
                Object converted = conversionService.convert(value, typeDescriptor, STRING_TYPE_DESCRIPTOR);
                return (String) converted;
            };
        }
    }

    /**
     * 传给各个AnnotatedParameterProcessor的上下文对象。
     */
    private class SimpleAnnotatedParameterContext implements AnnotatedParameterProcessor.AnnotatedParameterContext {
        private final MethodMetadata methodMetadata;

        private final int parameterIndex;

        SimpleAnnotatedParameterContext(MethodMetadata methodMetadata, int parameterIndex) {
            this.methodMetadata = methodMetadata;
            this.parameterIndex = parameterIndex;
        }

        @Override
        public MethodMetadata getMethodMetadata() {
            return methodMetadata;
        }

        @Override
        public int getParameterIndex() {
            return parameterIndex;
        }

        @Override
        public void setParameterName(String name) {
            nameParam(methodMetadata, name, parameterIndex);
        }

        @Override
        public Collection<String> setTemplateParameter(String name, Collection<String> rest) {
            return FeignUtils.addTemplateParameter(rest, name);
        }
    }

    /**
     * 用来解析@GetMapping(params = "a=1")或者@GetMapping(params = "!a")这样的值，会把表达式拆成：
     * 1、name
     * 2、value
     * 3、isNegated
     */
    private static class NameValueResolver {

        private final String name;

        private final String value;

        private final boolean isNegated;

        NameValueResolver(String expression) {
            int separator = expression.indexOf('=');
            if (separator == -1) {
                isNegated = expression.startsWith("!");
                name = (isNegated ? expression.substring(1) : expression);
                value = null;
            } else {
                isNegated = (separator > 0) && (expression.charAt(separator - 1) == '!');
                name = (isNegated ? expression.substring(0, separator - 1) : expression.substring(0, separator));
                value = expression.substring(separator + 1);
            }
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public boolean isNegated() {
            return isNegated;
        }

    }
}
