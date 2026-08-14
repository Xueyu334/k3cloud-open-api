package com.rain.starter;

import com.kingdee.bos.webapi.common.utils.api.http.SessionWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.http.SignedWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.sdk.WebApiHelper;
import com.kingdee.bos.webapi.config.properties.WebApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 在应用启动完成后确认 Starter 的核心 Bean 已成功注入。
 *
 * @author xueyu
 * @since 3.0.0
 */
@Component
public class StarterReadyListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarterReadyListener.class);

    private final WebApiProperties webApiProperties;
    private final WebApiHelper webApiHelper;
    private final SessionWebApiHttpHelper sessionWebApiHttpHelper;
    private final SignedWebApiHttpHelper signedWebApiHttpHelper;

    /**
     * 创建 Starter 启动完成监听器。
     *
     * @param webApiProperties Web API 配置 Bean
     * @param webApiHelper SDK 调用辅助 Bean
     * @param sessionWebApiHttpHelper 会话认证 HTTP Bean
     * @param signedWebApiHttpHelper 逐请求签名 HTTP Bean
     */
    public StarterReadyListener(WebApiProperties webApiProperties,
                                WebApiHelper webApiHelper,
                                SessionWebApiHttpHelper sessionWebApiHttpHelper,
                                SignedWebApiHttpHelper signedWebApiHttpHelper) {
        this.webApiProperties = webApiProperties;
        this.webApiHelper = webApiHelper;
        this.sessionWebApiHttpHelper = sessionWebApiHttpHelper;
        this.signedWebApiHttpHelper = signedWebApiHttpHelper;
    }

    /**
     * 输出应用启动耗时和已装配的 Starter 核心类型，不记录连接凭据。
     *
     * @param event 应用启动完成事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        Duration timeTaken = event.getTimeTaken();
        long elapsedMillis = timeTaken == null ? -1L : timeTaken.toMillis();
        String applicationName = event.getApplicationContext()
                .getEnvironment()
                .getProperty("spring.application.name", "k3cloud-open-api-spring-boot-test");
        LOGGER.info(
                "应用 {} 启动完成，耗时 {} ms；Starter Bean 已就绪：{}, {}, {}, {}",
                applicationName,
                elapsedMillis,
                webApiProperties.getClass().getSimpleName(),
                webApiHelper.getClass().getSimpleName(),
                sessionWebApiHttpHelper.getClass().getSimpleName(),
                signedWebApiHttpHelper.getClass().getSimpleName());
    }
}
