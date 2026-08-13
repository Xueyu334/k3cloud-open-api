package com.kingdee.bos.webapi.autoconfigure;

import com.kingdee.bos.webapi.common.utils.WebApiHelper;
import com.kingdee.bos.webapi.common.utils.WebApiHttpHelper;
import com.kingdee.bos.webapi.config.properties.WebApiProperties;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class K3CloudWebApiAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "kingdee.k3cloud.web-api.server-url=http://localhost/",
                    "kingdee.k3cloud.web-api.acct-id=test-acct",
                    "kingdee.k3cloud.web-api.user-name=test-user",
                    "kingdee.k3cloud.web-api.app-id=test-app",
                    "kingdee.k3cloud.web-api.app-sec=test-secret",
                    "kingdee.k3cloud.web-api.print-execute-url=false"
            );

    @Test
    void shouldNotCreateWebApiBeansByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(WebApiProperties.class);
            assertThat(context).doesNotHaveBean(K3CloudApi.class);
            assertThat(context).doesNotHaveBean(WebApiHttpHelper.class);
            assertThat(context).doesNotHaveBean(WebApiHelper.class);
        });
    }

    @Test
    void shouldBindPropertiesAndCreateWebApiBeans() {
        contextRunner.withUserConfiguration(EnabledConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WebApiProperties.class);
            assertThat(context).hasSingleBean(K3CloudApi.class);
            assertThat(context).hasSingleBean(WebApiHttpHelper.class);
            assertThat(context).hasSingleBean(WebApiHelper.class);

            WebApiProperties properties = context.getBean(WebApiProperties.class);
            assertThat(properties.getServerUrl()).isEqualTo("http://localhost/");
            assertThat(properties.getAcctId()).isEqualTo("test-acct");
            assertThat(properties.getLcId()).isEqualTo(2052);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableK3CloudWebApi
    static class EnabledConfiguration {
    }
}
