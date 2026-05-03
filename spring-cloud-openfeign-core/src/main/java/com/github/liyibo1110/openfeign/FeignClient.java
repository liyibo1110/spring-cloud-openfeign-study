package com.github.liyibo1110.openfeign;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于声明应创建具有该接口的REST客户端的接口注解（例如，用于自动注入到另一个组件中）。
 * 如果 SC LoadBalancer可用，则将使用它对后端请求进行负载均衡，且负载均衡器可使用与Feign客户端相同的名称（即值）进行配置。
 * @author liyibo
 * @date 2026-05-02 22:29
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface FeignClient {

    /**
     * 带可选协议前缀的服务名称，与name同义。
     * 无论是否提供了URL，都必须为所有客户端指定一个名称。可以作为属性键指定，例如：${propertyKey}。
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 如果存在，这将作为Bean名称（而非name），但不会用作服务ID。
     */
    String contextId() default "";

    @AliasFor("value")
    String name() default "";

    /**
     * feign client的@Qualifiers值。
     */
    String[] qualifiers() default {};

    /**
     * 绝对URL或可解析的主机名（协议可选）。
     */
    String url() default "";

    /**
     * 是否应该解析404错误，而不是抛出FeignExceptions。
     */
    boolean dismiss404() default false;

    /**
     * Feign客户端的自定义配置类。
     * 可以包含对客户端组成部分的@Bean定义的重写，例如feign.codec.Decoder、feign.codec.Encoder和feign.Contract。
     */
    Class<?>[] configuration() default {};

    /**
     * 指定Feign客户端接口的备用类。
     * 该备用类必须实现由该注解标注的接口，并且是一个有效的Spring Bean。
     */
    Class<?> fallback() default void.class;

    /**
     * 为指定的Feign客户端接口定义一个备用工厂。
     * 该备用工厂必须生成实现FeignClient注解接口的备用类实例。
     * 该备用工厂必须是一个有效的Spring Bean。
     */
    Class<?> fallbackFactory() default void.class;

    /**
     * 所有方法级映射将使用的路径前缀。
     */
    String path() default "";

    /**
     * 是否将feign proxy标记为主Bean。
     */
    boolean primary() default true;
}
