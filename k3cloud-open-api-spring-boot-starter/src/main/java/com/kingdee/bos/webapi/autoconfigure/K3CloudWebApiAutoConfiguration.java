package com.kingdee.bos.webapi.autoconfigure;

import com.kingdee.bos.webapi.common.utils.CfgUtilExt;
import com.kingdee.bos.webapi.common.utils.WebApiHelper;
import com.kingdee.bos.webapi.common.utils.WebApiHttpHelper;
import com.kingdee.bos.webapi.config.properties.WebApiProperties;
import com.kingdee.bos.webapi.entity.AppCfg;
import com.kingdee.bos.webapi.entity.IdentifyInfo;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 金蝶云星空 Web API 配置。
 *
 * @author xueyu
 * @see WebApiProperties
 * @see CfgUtilExt
 * @see WebApiHelper
 * @see EnableK3CloudWebApi
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(K3CloudApi.class)
@EnableConfigurationProperties
public class K3CloudWebApiAutoConfiguration {

    private static final String WEB_API_PROPERTIES_PREFIX = "kingdee.k3cloud.web-api";

    /**
     * 创建并绑定金蝶云星空 Web API 配置。
     *
     * @return Web API 配置
     */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = WEB_API_PROPERTIES_PREFIX)
    public WebApiProperties webApiProperties() {
        return new WebApiProperties();
    }

    /**
     * 配置云星空 Open API 客户端。
     *
     * @param webApiProperties 配置参数
     * @return 云星空 Open API 客户端
     */
    @Bean(name = "k3CloudApiClient")
    @ConditionalOnMissingBean(K3CloudApi.class)
    public K3CloudApi k3CloudApiClient(WebApiProperties webApiProperties) {
        log.info("开始配置金蝶云星空WebApi-AppClient==》");
        String serverUrl = webApiProperties.getServerUrl();
        if (StringUtils.isBlank(serverUrl)) {
            throw new IllegalArgumentException("云星空的服务URL不能为空!");
        }
        String acctId = webApiProperties.getAcctId();
        if (StringUtils.isBlank(acctId)) {
            throw new IllegalArgumentException("云星空的账套ID不能为空!");
        }
        String userName = webApiProperties.getUserName();
        String appId = webApiProperties.getAppId();
        if (StringUtils.isBlank(appId)) {
            throw new IllegalArgumentException("请填写云星空的授权应用APPID");
        }
        String appSec = webApiProperties.getAppSec();
        if (StringUtils.isBlank(appSec)) {
            throw new IllegalArgumentException("请填写云星空的授权应用密钥");
        }
        int lcId = webApiProperties.getLcId();
        String orgNum = webApiProperties.getOrgNum();
        int connectTimeout = webApiProperties.getConnectTimeout();
        int requestTimeout = webApiProperties.getRequestTimeout();
        int stockTimeout = webApiProperties.getStockTimeout();
        String proxy = webApiProperties.getProxy();

        AppCfg appCfg = CfgUtilExt.builder()
                .serverUrl(serverUrl)
                .acctId(acctId)
                .userName(userName)
                .appId(appId)
                .appSecret(appSec)
                .lcId(lcId)
                .orgNum(orgNum)
                .connectTimeout(connectTimeout)
                .requestTimeout(requestTimeout)
                .stockTimeout(stockTimeout)
                .proxy(proxy)
                .build();
        // 设置 SDK 的 JVM 全局配置，供 HttpUtils#getProxy() 获取代理信息。
        CfgUtilExt.setAppCfgToCfgUtil(appCfg);
        IdentifyInfo identifyInfo = new IdentifyInfo();
        identifyInfo.copyPropertiesToAppCfg(appCfg);
        return new K3CloudApi(identifyInfo, webApiProperties.isPrintExecuteUrl());
    }

    /**
     * 创建金蝶云星空 Web API HTTP 请求辅助工具。
     *
     * @param webApiProperties 配置参数
     * @return HTTP 请求辅助工具
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public WebApiHttpHelper k3CloudWebApiHttpHelper(WebApiProperties webApiProperties) {
        return WebApiHttpHelper.of(webApiProperties);
    }

    /**
     * 创建金蝶云星空 Web API 辅助工具。
     *
     * @param k3CloudApi Open API 客户端
     * @return Web API 辅助工具
     */
    @Bean
    @ConditionalOnMissingBean
    public WebApiHelper k3CloudWebApiHelper(K3CloudApi k3CloudApi) {
        return WebApiHelper.of(k3CloudApi);
    }
}
