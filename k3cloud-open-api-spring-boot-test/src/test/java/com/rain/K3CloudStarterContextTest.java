package com.rain;

import com.kingdee.bos.webapi.common.utils.api.http.SessionWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.http.SignedWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.sdk.WebApiHelper;
import com.kingdee.bos.webapi.config.properties.WebApiProperties;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证业务应用引入 Starter 后可以完成配置绑定和核心 Bean 装配。
 */
@SpringBootTest(classes = K3CloudOpenApiStarterTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class K3CloudStarterContextTest {

    private final ApplicationContext applicationContext;

    @Autowired
    K3CloudStarterContextTest(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Test
    void shouldLoadStarterBeans() {
        assertThat(applicationContext.getBeansOfType(WebApiProperties.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(K3CloudApi.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(WebApiHelper.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(SessionWebApiHttpHelper.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(SignedWebApiHttpHelper.class)).hasSize(1);
    }
}
