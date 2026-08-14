package com.kingdee.bos.webapi.autoconfigure;

import com.kingdee.bos.webapi.common.utils.api.http.SessionWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.http.SignedWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.sdk.WebApiHelper;
import com.kingdee.bos.webapi.config.properties.WebApiProperties;
import com.kingdee.bos.webapi.entity.AppCfg;
import com.kingdee.bos.webapi.entity.IdentifyInfo;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.kingdee.bos.webapi.utils.CfgUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class K3CloudWebApiAutoConfigurationTest {

    private Object originalGlobalConfiguration;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "kingdee.k3cloud.web-api.server-url=http://localhost/",
                    "kingdee.k3cloud.web-api.acct-id=test-acct",
                    "kingdee.k3cloud.web-api.user-name=test-user",
                    "kingdee.k3cloud.web-api.app-id=test-app",
                    "kingdee.k3cloud.web-api.app-sec=test-secret",
                    "kingdee.k3cloud.web-api.proxy=http://127.0.0.1:8080",
                    "kingdee.k3cloud.web-api.print-execute-url=false"
            );

    @BeforeEach
    void captureSdkGlobalConfiguration() {
        originalGlobalConfiguration = ReflectionTestUtils.getField(CfgUtil.class, "instance");
    }

    @AfterEach
    void restoreSdkGlobalConfiguration() {
        ReflectionTestUtils.setField(CfgUtil.class, "instance", originalGlobalConfiguration);
    }

    @Test
    void shouldNotCreateWebApiBeansByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(WebApiProperties.class);
            assertThat(context).doesNotHaveBean(K3CloudApi.class);
            assertThat(context).doesNotHaveBean(SessionWebApiHttpHelper.class);
            assertThat(context).doesNotHaveBean(SignedWebApiHttpHelper.class);
            assertThat(context).doesNotHaveBean(WebApiHelper.class);
        });
    }

    @Test
    void shouldBindPropertiesAndCreateWebApiBeans() {
        contextRunner.withUserConfiguration(EnabledConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WebApiProperties.class);
            assertThat(context).hasSingleBean(K3CloudApi.class);
            assertThat(context).hasSingleBean(SessionWebApiHttpHelper.class);
            assertThat(context).hasSingleBean(SignedWebApiHttpHelper.class);
            assertThat(context).hasSingleBean(WebApiHelper.class);

            WebApiProperties properties = context.getBean(WebApiProperties.class);
            assertThat(properties.getServerUrl()).isEqualTo("http://localhost/");
            assertThat(properties.getAcctId()).isEqualTo("test-acct");
            assertThat(properties.getLcId()).isEqualTo(2052);

            K3CloudApi k3CloudApi = context.getBean(K3CloudApi.class);
            assertThat(ReflectionTestUtils.getField(k3CloudApi, "identify"))
                    .isInstanceOfSatisfying(IdentifyInfo.class, identifyInfo -> {
                        assertThat(identifyInfo.getServerUrl()).isEqualTo("http://localhost/");
                        assertThat(identifyInfo.getdCID()).isEqualTo("test-acct");
                        assertThat(identifyInfo.getUserName()).isEqualTo("test-user");
                        assertThat(identifyInfo.getAppId()).isEqualTo("test-app");
                        assertThat(identifyInfo.getAppSecret()).isEqualTo("test-secret");
                        assertThat(identifyInfo.getlCID()).isEqualTo(2052);
                    });
        });
    }

    @Test
    void shouldSetSdkGlobalConfiguration() {
        contextRunner.withUserConfiguration(EnabledConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(ReflectionTestUtils.getField(CfgUtil.class, "instance"))
                    .isInstanceOfSatisfying(AppCfg.class, appCfg -> {
                        assertThat(appCfg.getServerUrl()).isEqualTo("http://localhost/");
                        assertThat(appCfg.getdCID()).isEqualTo("test-acct");
                        assertThat(appCfg.getAppId()).isEqualTo("test-app");
                        assertThat(appCfg.getProxy()).isEqualTo("http://127.0.0.1:8080");
                    });
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableK3CloudWebApi
    static class EnabledConfiguration {
    }
}
