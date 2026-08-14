package com.kingdee.bos.webapi.common.utils.api.http;

import com.alibaba.fastjson2.JSON;
import com.kingdee.bos.webapi.common.convert.WebApiResponseConverter;
import com.kingdee.bos.webapi.common.enums.WebApiService;
import com.kingdee.bos.webapi.common.exception.WebApiInvokeException;
import com.kingdee.bos.webapi.config.properties.WebApiProperties;
import com.kingdee.bos.webapi.domain.dto.request.save.SaveRequest;
import com.kingdee.bos.webapi.domain.dto.response.WebApiResp;
import com.kingdee.bos.webapi.domain.dto.response.result.SaveResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 抽象类 AbstractWebApiHttpHelper 是一个用于与金蝶 K3Cloud 平台 Web API 交互的辅助工具类。
 * 提供 HTTP 请求执行、响应处理以及会话管理等与 Web API 相关的功能支持。
 * <p>
 * 子类通过继承此类，可以扩展和自定义具体的 API 调用行为。
 *
 * @author xueyu
 * @since 3.0.0
 */
@Slf4j
public abstract class AbstractWebApiHttpHelper implements AutoCloseable {

    /**
     * Web API 配置。
     */
    protected final WebApiProperties webApiProperties;

    /**
     * Web API 响应转换器。
     */
    protected final WebApiResponseConverter webApiResponseConverter;
    /**
     * 用于存储和管理 HTTP 请求中的 Cookie 信息。
     * <p>
     * 该变量是基于 Apache HttpClient 的 {@code CookieStore} 接口实现，
     * 负责在处理 HTTP 请求和响应时维护 Cookie 的会话状态。
     * <p>
     * 在辅助类中，这一存储通常被用以支持会话管理和跨请求的认证。
     * Cookie 的生命周期和管理逻辑则由 {@code BasicCookieStore} 默认实现负责。
     * <p>
     * 作为不可变成员变量，`cookieStore` 的实例在创建过程中已被初始化，
     * 并在辅助类的整个生命周期中始终可用。
     */
    private final CookieStore cookieStore = new BasicCookieStore();
    /**
     * 表示一个可关闭的 HTTP 客户端，用于发送 HTTP 请求和接收 HTTP 响应。
     * <p>
     * 该客户端支持连接池和 Cookie 管理，适合需要复用连接和维护会话状态的场景。
     * 主要用于调用 Web API 服务，可通过扩展类自定义请求行为和认证逻辑。
     */
    private CloseableHttpClient httpClient;


    /**
     * 构造函数，用于初始化 AbstractWebApiHttpHelper 类的实例。
     * 该方法接收必要的配置参数和响应转换器，并完成 HTTP 客户端的初始化。
     *
     * @param webApiProperties        云星空 WebApi 配置信息，不可为 null
     * @param webApiResponseConverter 响应消息转换器，用于解析 API 响应，不可为 null
     * @throws NullPointerException 如果 webApiProperties 或 webApiResponseConverter 为 null，则抛出该异常
     */
    protected AbstractWebApiHttpHelper(WebApiProperties webApiProperties, WebApiResponseConverter webApiResponseConverter) {
        this.webApiProperties = Objects.requireNonNull(webApiProperties, "云星空WebApi客户端不能为空!");
        this.webApiResponseConverter = Objects.requireNonNull(webApiResponseConverter, "响应消息转换插件不能为空!");
        initHttpClient();
    }

    /**
     * 执行指定的金蝶 K3Cloud Web API 服务并返回其响应数据。
     * <p>
     * 该方法包括参数校验、预处理逻辑和实际 Web API 请求的执行，最终返回处理后的响应数据。
     *
     * @param serviceName API 服务名称，不能为空且不能为空白字符串
     * @param parameters  API 请求参数数组，不能为空
     * @return API 响应数据字符串
     * @throws IllegalArgumentException 如果 serviceName 为空或为空白字符串，抛出此异常
     * @throws IllegalArgumentException 如果 parameters 为空，抛出此异常
     * @throws WebApiInvokeException    如果 HTTP 请求失败、服务端返回错误状态或发生 I/O 异常，抛出此异常
     */
    public final String execute(String serviceName, Object[] parameters) {
        validateRequest(serviceName, parameters);
        beforeExecute();
        return executeRaw(serviceName, parameters);
    }

    /**
     * 在执行 API 请求之前执行的预处理逻辑。
     * <p>
     * 此方法由框架调用，用于在 HTTP 请求正式发出前执行一些通用的初始化操作，
     * 或提供给子类用于扩展和自定义行为。
     * <p>
     * 子类可以重写此方法，以便于执行如以下场景：
     * <ul>
     * - 日志记录或性能监控。
     * - 动态调整配置参数。
     * - 检查或修改全局状态。
     * - 预热或测试连接。
     * <p>
     * 默认实现不执行任何操作。
     */
    protected void beforeExecute() {
    }

    /**
     * 为即将发送的请求补充认证信息。
     *
     * @param request    HTTP POST 请求
     * @param requestUrl 完整请求地址
     */
    protected void customizeRequest(HttpPost request, String requestUrl) {
    }

    /**
     * 执行指定的金蝶 K3Cloud Web API 服务并返回原始响应数据。
     *
     * @param serviceName API 服务名称，不能为空且不能为空白字符串
     * @param parameters  API 请求参数数组，不能为空
     * @return API 原始响应字符串
     * @throws IllegalArgumentException 如果 serviceName 为空或为空白字符串，抛出异常
     * @throws IllegalArgumentException 如果 parameters 为空，抛出异常
     * @throws WebApiInvokeException    如果 HTTP 请求失败、服务端返回 HTTP 错误状态或发生 I/O 异常，抛出该异常
     */
    protected final String executeRaw(String serviceName, Object[] parameters) {
        validateRequest(serviceName, parameters);
        String requestUrl = getServiceUrl(serviceName);
        if (webApiProperties.isPrintExecuteUrl()) {
            log.info("KingDee K3Cloud Web API Execute URL: {}", requestUrl);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("parameters", parameters);
        HttpPost httpPost = new HttpPost(requestUrl);
        customizeRequest(httpPost, requestUrl);
        httpPost.setEntity(new StringEntity(JSON.toJSONString(body),
                ContentType.APPLICATION_JSON, StandardCharsets.UTF_8.name(), false));
        try {
            return httpClient.execute(httpPost, response -> {
                HttpEntity entity = response.getEntity();
                String responseBody = entity == null ? "" : EntityUtils.toString(entity);
                int statusCode = response.getCode();
                if (statusCode >= 400) {
                    throw new WebApiInvokeException(statusCode,
                            "调用K3Cloud Web API失败，HTTP状态码：" + statusCode + "，响应：" + responseBody);
                }
                return responseBody;
            });
        } catch (IOException e) {
            log.error("调用K3Cloud Web API 出现异常!", e);
            throw new WebApiInvokeException("调用K3Cloud Web API 出现异常!", e);
        }
    }

    /**
     * 执行单据保存。
     *
     * @param formId 表单标识
     * @param data   保存请求
     * @return 保存响应
     */
    public final WebApiResp<SaveResult> save(String formId, SaveRequest data) {
        Object[] parameters = new Object[]{formId, JSON.toJSONString(data)};
        String response = execute(WebApiService.SAVE.getServiceName(), parameters);
        return webApiResponseConverter.parseSaveWebApiResponse(response);
    }

    /**
     * 执行单据查询服务，调用指定的金蝶 K3Cloud Web API 服务并返回查询结果。
     * 方法处理查询请求、响应解析及错误捕获。
     *
     * @param data 单据查询的请求参数，不能为空或空白字符串
     * @return 返回查询结果，类型为 List<List<Object>>；如果查询未返回数据或发生异常，返回空列表
     * @throws IllegalArgumentException 当参数 data 为空或为空白字符串时，抛出此异常
     * @throws WebApiInvokeException    当调用 Web API 或解析响应数据失败时，抛出此异常
     */
    public final List<List<Object>> executeBillQuery(String data) {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("executeBillQuery 参数 data 不能为空");
        }
        String response = execute(WebApiService.EXECUTE_BILL_QUERY.getServiceName(), new Object[]{data});
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return webApiResponseConverter.parseListListObjectApiResponse(response);
        } catch (Exception e) {
            throw new WebApiInvokeException("解析 ExecuteBillQuery 返回结果失败，原始响应：" + response, e);
        }
    }

    /**
     * 根据指定的服务名称生成完整的服务 URL。
     *
     * @param serviceName 服务名称，用于标识具体的 API 服务
     * @return 拼接的完整服务 URL 字符串，包含服务名称和默认后缀 ".common.kdsvc"
     */
    protected final String getServiceUrl(String serviceName) {
        String serverUrl = webApiProperties.getServerUrl();
        if (serverUrl == null) {
            serverUrl = "";
        }
        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }
        return serverUrl + serviceName + ".common.kdsvc";
    }

    /**
     * 清理认证状态。
     * <p>
     * 此方法用于在认证状态重置或会话终止时调用，以移除可能存在的认证相关数据。
     * 子类可重写此方法以执行特定的清理逻辑，例如清除缓存的会话令牌、重置认证标志等。
     * <p>
     * 默认情况下，此方法不执行任何操作。
     */
    protected void clearAuthenticationState() {
    }

    /**
     * 关闭当前 HTTP 辅助类的资源。
     * 该方法确保释放 HTTP 客户端资源、清空 Cookie 存储，并清理认证状态。
     * <p>
     * 具体实现包括：
     * 1. 关闭 HTTP 客户端实例以释放网络连接和相关资源。
     * 2. 清空 Cookie 存储以移除会话相关数据。
     * 3. 调用 {@code clearAuthenticationState()} 方法，以清理子类可能存在的额外认证状态。
     * <p>
     * 此方法在关闭时总是将 HTTP 客户端引用设置为 {@code null}，以防止重复调用而导致意外行为。
     *
     * @throws IOException 如果关闭 HTTP 客户端时发生 I/O 异常，抛出该异常。
     */
    @Override
    public final void close() throws IOException {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } finally {
            httpClient = null;
            cookieStore.clear();
            clearAuthenticationState();
        }
    }

    /**
     * 初始化 HTTP 客户端配置，包括连接池、请求配置和 Cookie 存储。
     * <p>
     * - 创建自定义的请求配置，设置连接请求超时和响应超时的阈值。
     * - 创建连接配置，设置连接超时时间。
     * - 配置连接池管理器，定义最大连接数和每个路由的最大连接数。
     * - 构建带有默认连接管理器、请求配置和 Cookie 存储的 HTTP 客户端。
     * <p>
     * 方法用于设置满足业务需求的 HTTP 请求功能及资源管理，确保系统高效运行。
     */
    private void initHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(webApiProperties.getRequestTimeout()))
                .setResponseTimeout(Timeout.ofSeconds(webApiProperties.getStockTimeout()))
                .build();
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(webApiProperties.getConnectTimeout()))
                .build();
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(connectionConfig)
                        .setMaxConnTotal(120)
                        .setMaxConnPerRoute(35)
                        .build();
        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultCookieStore(cookieStore)
                .build();
    }

    /**
     * 验证 API 请求参数的有效性。
     *
     * @param serviceName API 服务名称，不能为空且不能为空白字符串
     * @param parameters  API 请求参数数组，不能为空
     * @throws IllegalArgumentException 如果 serviceName 为空或为空白字符串，抛出异常
     * @throws IllegalArgumentException 如果 parameters 为空，抛出异常
     */
    private void validateRequest(String serviceName, Object[] parameters) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName 不能为空");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("parameters 不能为空");
        }
    }
}
