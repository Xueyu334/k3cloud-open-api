package com.kingdee.bos.webapi.autoconfigure;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用金蝶云星空 Web API 配置。
 *
 * <p>将此注解添加到 Spring 配置类或应用启动类后，才会注册金蝶云星空
 * Web API 的配置属性、客户端及辅助工具。</p>
 *
 * @author xueyu
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(K3CloudWebApiAutoConfiguration.class)
public @interface EnableK3CloudWebApi {
}
