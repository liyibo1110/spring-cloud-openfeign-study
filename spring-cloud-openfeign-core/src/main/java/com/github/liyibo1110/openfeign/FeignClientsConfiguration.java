package com.github.liyibo1110.openfeign;

import com.github.liyibo1110.openfeign.clientconfig.FeignClientConfigurer;
import com.github.liyibo1110.openfeign.support.AbstractFormWriter;
import com.github.liyibo1110.openfeign.support.FeignEncoderProperties;
import com.github.liyibo1110.openfeign.support.HttpMessageConverterCustomizer;
import com.github.liyibo1110.openfeign.support.PageableSpringEncoder;
import com.github.liyibo1110.openfeign.support.PageableSpringQueryMapEncoder;
import com.github.liyibo1110.openfeign.support.ResponseEntityDecoder;
import com.github.liyibo1110.openfeign.support.SpringDecoder;
import com.github.liyibo1110.openfeign.support.SpringEncoder;
import com.github.liyibo1110.openfeign.support.SpringMvcContract;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.ContentType;
import feign.form.MultipartFormContentProcessor;
import feign.form.spring.SpringFormEncoder;
import feign.micrometer.MicrometerCapability;
import feign.micrometer.MicrometerObservationCapability;
import feign.optionals.OptionalDecoder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;

import java.util.ArrayList;
import java.util.List;

/**
 * 提供单个Feign Client所需的默认组件。
 * @author liyibo
 * @date 2026-05-06 13:37
 */
@Configuration(proxyBeanMethods = false)
public class FeignClientsConfiguration {

    @Autowired
    private ObjectFactory<HttpMessageConverters> messageConverters;

    @Autowired(required = false)
    private List<AnnotatedParameterProcessor> parameterProcessors = new ArrayList<>();

    @Autowired(required = false)
    private List<FeignFormatterRegistrar> feignFormatterRegistrars = new ArrayList<>();

    @Autowired(required = false)
    private Logger logger;

    @Autowired(required = false)
    private SpringDataWebProperties springDataWebProperties;

    @Autowired(required = false)
    private FeignClientProperties feignClientProperties;

    @Autowired(required = false)
    private FeignEncoderProperties encoderProperties;

    @Bean
    @ConditionalOnMissingBean
    public Decoder feignDecoder(ObjectProvider<HttpMessageConverterCustomizer> customizers) {
        return new OptionalDecoder(new ResponseEntityDecoder(new SpringDecoder(messageConverters, customizers)));
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnMissingClass("org.springframework.data.domain.Pageable")
    public Encoder feignEncoder(ObjectProvider<AbstractFormWriter> formWriterProvider,
                                ObjectProvider<HttpMessageConverterCustomizer> customizers) {
        return springEncoder(formWriterProvider, encoderProperties, customizers);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.data.domain.Pageable")
    @ConditionalOnMissingBean
    public Encoder feignEncoderPageable(ObjectProvider<AbstractFormWriter> formWriterProvider,
                                        ObjectProvider<HttpMessageConverterCustomizer> customizers) {
        PageableSpringEncoder encoder = new PageableSpringEncoder(springEncoder(formWriterProvider, encoderProperties, customizers));

        if (springDataWebProperties != null) {
            encoder.setPageParameter(springDataWebProperties.getPageable().getPageParameter());
            encoder.setSizeParameter(springDataWebProperties.getPageable().getSizeParameter());
            encoder.setSortParameter(springDataWebProperties.getSort().getSortParameter());
        }
        return encoder;
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.data.domain.Pageable")
    @ConditionalOnMissingBean
    public QueryMapEncoder feignQueryMapEncoderPageable() {
        PageableSpringQueryMapEncoder queryMapEncoder = new PageableSpringQueryMapEncoder();
        if (springDataWebProperties != null) {
            queryMapEncoder.setPageParameter(springDataWebProperties.getPageable().getPageParameter());
            queryMapEncoder.setSizeParameter(springDataWebProperties.getPageable().getSizeParameter());
            queryMapEncoder.setSortParameter(springDataWebProperties.getSort().getSortParameter());
        }
        return queryMapEncoder;
    }

    @Bean
    @ConditionalOnMissingBean
    public Contract feignContract(ConversionService feignConversionService) {
        return new SpringMvcContract(parameterProcessors, feignConversionService, feignClientProperties);
    }

    @Bean
    public FormattingConversionService feignConversionService() {
        FormattingConversionService conversionService = new DefaultFormattingConversionService();
        for (FeignFormatterRegistrar feignFormatterRegistrar : feignFormatterRegistrars)
            feignFormatterRegistrar.registerFormatters(conversionService);

        return conversionService;
    }

    @Bean
    @ConditionalOnMissingBean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    @ConditionalOnMissingBean(FeignLoggerFactory.class)
    public FeignLoggerFactory feignLoggerFactory() {
        return new DefaultFeignLoggerFactory(logger);
    }

    @Bean
    @ConditionalOnMissingBean(FeignClientConfigurer.class)
    public FeignClientConfigurer feignClientConfigurer() {
        return new FeignClientConfigurer() {

        };
    }

    private Encoder springEncoder(ObjectProvider<AbstractFormWriter> formWriterProvider,
                                  FeignEncoderProperties encoderProperties, ObjectProvider<HttpMessageConverterCustomizer> customizers) {
        AbstractFormWriter formWriter = formWriterProvider.getIfAvailable();

        if (formWriter != null)
            return new SpringEncoder(new SpringPojoFormEncoder(formWriter), messageConverters, encoderProperties, customizers);
        else
            return new SpringEncoder(new SpringFormEncoder(), messageConverters, encoderProperties, customizers);
    }

    private class SpringPojoFormEncoder extends SpringFormEncoder {
        SpringPojoFormEncoder(AbstractFormWriter formWriter) {
            super();
            MultipartFormContentProcessor processor = (MultipartFormContentProcessor) getContentProcessor(ContentType.MULTIPART);
            processor.addFirstWriter(formWriter);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Conditional(FeignCircuitBreakerDisabledConditions.class)
    protected static class DefaultFeignBuilderConfiguration {
        @Bean
        @Scope("prototype")
        @ConditionalOnMissingBean
        public Feign.Builder feignBuilder(Retryer retryer) {
            return Feign.builder().retryer(retryer);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(CircuitBreaker.class)
    @ConditionalOnProperty("spring.cloud.openfeign.circuitbreaker.enabled")
    protected static class CircuitBreakerPresentFeignBuilderConfiguration {
        @Bean
        @Scope("prototype")
        @ConditionalOnMissingBean({ Feign.Builder.class, CircuitBreakerFactory.class })
        public Feign.Builder defaultFeignBuilder(Retryer retryer) {
            return Feign.builder().retryer(retryer);
        }

        @Bean
        @Scope("prototype")
        @ConditionalOnMissingBean
        @ConditionalOnBean(CircuitBreakerFactory.class)
        public Feign.Builder circuitBreakerFeignBuilder() {
            return FeignCircuitBreaker.builder();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "spring.cloud.openfeign.micrometer.enabled", matchIfMissing = true)
    @ConditionalOnClass({ MicrometerObservationCapability.class, MicrometerCapability.class, MeterRegistry.class })
    @Conditional(FeignClientMicrometerEnabledCondition.class)
    protected static class MicrometerConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(type = "io.micrometer.observation.ObservationRegistry")
        public MicrometerObservationCapability micrometerObservationCapability(ObservationRegistry registry) {
            return new MicrometerObservationCapability(registry);
        }

        @Bean
        @ConditionalOnBean(type = "io.micrometer.core.instrument.MeterRegistry")
        @ConditionalOnMissingBean({ MicrometerCapability.class, MicrometerObservationCapability.class })
        public MicrometerCapability micrometerCapability(MeterRegistry registry) {
            return new MicrometerCapability(registry);
        }
    }
}
