package com.rain;

import com.kingdee.bos.webapi.common.utils.api.http.SessionWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.http.SignedWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.sdk.WebApiHelper;
import com.kingdee.bos.webapi.config.properties.WebApiProperties;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证业务应用引入 Starter 后可以完成配置绑定和核心 Bean 装配。
 */
@Slf4j
@SpringBootTest(classes = K3CloudOpenApiStarterTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class K3CloudStarterContextTest {

    private final ApplicationContext applicationContext;

    @Autowired
    K3CloudStarterContextTest(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Test
    void shouldLoadStarterBeans() {
        log.info("开始验证 K3 Cloud Open API Starter 核心 Bean");

        assertAndLogSingleBean(WebApiProperties.class);
        assertAndLogSingleBean(K3CloudApi.class);
        assertAndLogSingleBean(WebApiHelper.class);
        assertAndLogSingleBean(SessionWebApiHttpHelper.class);
        assertAndLogSingleBean(SignedWebApiHttpHelper.class);

        WebApiProperties properties = applicationContext.getBean(WebApiProperties.class);
        log.info(
                "Starter 配置绑定结果：serverUrlConfigured={}, acctIdConfigured={}, userNameConfigured={}, " +
                        "appIdConfigured={}, appSecretConfigured={}, lcId={}, orgNumConfigured={}, connectTimeout={}s, " +
                        "requestTimeout={}s, stockTimeout={}s, proxyConfigured={}, printExecuteUrl={}",
                hasText(properties.getServerUrl()),
                hasText(properties.getAcctId()),
                hasText(properties.getUserName()),
                hasText(properties.getAppId()),
                hasText(properties.getAppSec()),
                properties.getLcId(),
                hasText(properties.getOrgNum()),
                properties.getConnectTimeout(),
                properties.getRequestTimeout(),
                properties.getStockTimeout(),
                hasText(properties.getProxy()),
                properties.isPrintExecuteUrl());
        log.info("K3 Cloud Open API Starter 核心 Bean 验证完成");
    }

    private <T> void assertAndLogSingleBean(Class<T> beanType) {
        Map<String, T> beans = applicationContext.getBeansOfType(beanType);
        log.info("Bean 类型检查：type={}, count={}", beanType.getName(), beans.size());
        beans.forEach((beanName, bean) -> log.info(
                "Bean 装配结果：name={}, declaredType={}, implementation={}",
                beanName,
                beanType.getName(),
                bean.getClass().getName()));
        assertThat(beans).hasSize(1);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
