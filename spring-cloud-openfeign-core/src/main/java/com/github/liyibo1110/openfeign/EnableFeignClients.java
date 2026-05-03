package com.github.liyibo1110.openfeign;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 扫描声明为Feign客户端（通过@FeignClient注解）的接口。
 * 配置组件扫描指令，以便与@Configuration类配合使用。
 * @author liyibo
 * @date 2026-05-02 22:09
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface EnableFeignClients {

    /**
     * basePackages()属性的别名。
     * 可实现更简洁的注解声明，
     * 例如@ComponentScan("org.my.pkg")代替@ComponentScan(basePackages="org.my.pkg")。
     */
    String[] value() default {};

    /**
     * 用于扫描带注解组件的基础包。
     * value()是该属性的别名（且与之互斥）。
     * 若需使用类型安全的替代方案来替代基于字符串的包名，请使用basePackageClasses()。
     */
    String[] basePackages() default {};

    /**
     * 这是basePackages()的类型安全替代方案，用于指定要扫描以查找带注解组件的包。指定的每个类的包都将被扫描。
     * 建议在每个包中创建一个特殊的无操作标记类或接口，其唯一作用就是被此属性引用。
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * 适用于所有Feign客户端的自定义@Configuration。
     * 其中可包含对客户端组成部分的@Bean定义的覆盖，例如feign.codec.Decoder、feign.codec.Encoder和feign.Contract。
     */
    Class<?>[] defaultConfiguration() default {};

    /**
     * 标注了@FeignClient的类列表。若不为空，则禁用类路径扫描
     */
    Class<?>[] clients() default {};
}
