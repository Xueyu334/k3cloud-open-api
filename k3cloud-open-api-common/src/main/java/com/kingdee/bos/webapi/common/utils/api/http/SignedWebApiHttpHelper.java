package com.kingdee.bos.webapi.common.utils.api.http;

import com.kingdee.bos.webapi.common.convert.WebApiResponseConverter;
import com.kingdee.bos.webapi.common.convert.fastjson.FastJsonConvertApiResponse;
import com.kingdee.bos.webapi.common.exception.WebApiInvokeException;
import com.kingdee.bos.webapi.config.properties.WebApiProperties;
import org.apache.hc.client5.http.classic.methods.HttpPost;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 使用逐请求签名认证调用金蝶 K3Cloud Web API 的 HTTP 辅助类。
 * <p>
 * 本类负责按照金蝶 Java SDK 8.2.0 {@code ApiRequester#buildHeader} 的规则为每次
 * 请求生成鉴权请求头；公共 HTTP 执行与资源生命周期由
 * {@link AbstractWebApiHttpHelper} 管理。本实现不调用 {@code LoginBySign}，也不维护
 * 登录结果或主动执行会话心跳。
 * </p>
 *
 * @author xueyu
 * @see AbstractWebApiHttpHelper
 * @see SessionWebApiHttpHelper
 * @since 3.0.0
 */
public class SignedWebApiHttpHelper extends AbstractWebApiHttpHelper {

    private static final String AUTH_VERSION = "2.0";
    private static final String SIGN_HEADERS = "X-Api-TimeStamp,X-Api-Nonce";
    private static final String API_SECRET_XOR_KEY = "0054f397c6234378b09ca7d3e5debce7";

    /**
     * 使用指定配置和响应转换器创建逐请求签名客户端。
     *
     * @param webApiProperties        Web API 配置
     * @param webApiResponseConverter 响应转换器
     */
    private SignedWebApiHttpHelper(WebApiProperties webApiProperties,
                                   WebApiResponseConverter webApiResponseConverter) {
        super(webApiProperties, webApiResponseConverter);
    }

    /**
     * 使用默认 FastJSON2 响应转换器创建逐请求签名客户端。
     *
     * @param webApiProperties Web API 配置
     * @return HTTP 辅助类实例
     */
    public static SignedWebApiHttpHelper of(WebApiProperties webApiProperties) {
        return new SignedWebApiHttpHelper(webApiProperties, FastJsonConvertApiResponse.INSTANCE);
    }

    /**
     * 自定义 HTTP 请求方法。
     * 该方法通过调用 buildAuthHeaders 生成鉴权请求头，并将其添加到给定的 HTTP 请求中。
     *
     * @param request    HTTP POST 请求对象，用于设置自定义的请求头
     * @param requestUrl 完整请求地址，作为生成鉴权头信息的参数
     */
    @Override
    protected void customizeRequest(HttpPost request, String requestUrl) {
        buildAuthHeaders(requestUrl).forEach(request::setHeader);
    }

    /**
     * 按照金蝶 Java SDK 8.2.0 的规则为当前请求生成鉴权请求头。
     * {com.kingdee.bos.webapi.sdk.ApiRequester#buildHeader}
     *
     * @param requestUrl 完整请求地址
     * @return 鉴权请求头
     */
    Map<String, String> buildAuthHeaders(String requestUrl) {
        String appId = requireConfig(webApiProperties.getAppId(), "appId");
        String appSecret = requireConfig(webApiProperties.getAppSec(), "appSec");
        String acctId = requireConfig(webApiProperties.getAcctId(), "acctId");
        String userName = requireConfig(webApiProperties.getUserName(), "userName");

        Map<String, String> headers = new HashMap<>();
        String[] appIdParts = appId.split("_");
        String clientId = appIdParts[0];
        String apiSecret = appIdParts.length >= 2 ? decodeApiSecret(appIdParts[1]) : "";

        long timestamp = System.currentTimeMillis();
        String timestampText = Long.toString(timestamp);
        String nonce = Long.toString(timestamp);
        headers.put("X-Api-ClientID", clientId);
        headers.put("X-Api-Auth-Version", AUTH_VERSION);
        headers.put("x-api-timestamp", timestampText);
        headers.put("x-api-nonce", nonce);
        headers.put("x-api-signheaders", SIGN_HEADERS);

        String encodedPath = URLEncoder.encode(getUrlPath(requestUrl), StandardCharsets.UTF_8);
        String canonicalRequest = String.format(
                "POST\n%s\n\nx-api-nonce:%s\nx-api-timestamp:%s\n",
                encodedPath,
                nonce,
                timestampText);
        headers.put(
                "X-Api-Signature",
                apiSecret.isBlank() ? "" : hashMAC(canonicalRequest, apiSecret));

        String appData = String.format(
                "%s,%s,%s,%s",
                acctId,
                userName,
                webApiProperties.getLcId(),
                webApiProperties.getOrgNum());
        String encodedAppData = encodingToBase64(appData.getBytes(StandardCharsets.UTF_8));
        headers.put("X-Kd-Appkey", appId);
        headers.put("X-Kd-Appdata", encodedAppData);
        headers.put("X-Kd-Signature", hashMAC(appId + appData, appSecret));
        return headers;
    }

    /**
     * 解码应用 ID 中携带的 API 签名密钥。
     *
     * @param encodedSecret 编码后的密钥
     * @return SDK 签名算法使用的密钥
     */
    private String decodeApiSecret(String encodedSecret) {
        byte[] secret = decodingFromBase64(encodedSecret);
        byte[] xorKey = API_SECRET_XOR_KEY.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < secret.length; i++) {
            secret[i] = (byte) (secret[i] ^ xorKey[i]);
        }
        return encodingToBase64(secret);
    }

    /**
     * 使用标准 Base64 编码字节数组。
     *
     * @param data 待编码数据
     * @return Base64 字符串
     */
    private String encodingToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * 使用标准 Base64 解码字符串。
     *
     * @param data Base64 字符串
     * @return 解码后的字节数组
     */
    private byte[] decodingFromBase64(String data) {
        return Base64.getDecoder().decode(data);
    }

    /**
     * 使用 HmacSHA256 生成签名，并按照金蝶 SDK 的格式返回结果。
     * <p>
     * SDK 会先把摘要转为小写十六进制字符串，再对该字符串执行 Base64 编码。
     * </p>
     *
     * @param content 待签名内容
     * @param secret  签名密钥
     * @return Base64 编码的签名
     * @throws WebApiInvokeException 当前运行环境不支持 HmacSHA256 时抛出
     */
    private String hashMAC(String content, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return encodingToBase64(bytesToHex(digest).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new WebApiInvokeException("生成 HmacSHA256 签名失败", e);
        }
    }

    /**
     * 将字节数组转换为小写十六进制字符串。
     *
     * @param data 待转换数据
     * @return 小写十六进制字符串
     */
    private String bytesToHex(byte[] data) {
        StringBuilder hex = new StringBuilder(data.length * 2);
        for (byte value : data) {
            String item = Integer.toHexString(value & 0xff);
            if (item.length() < 2) {
                hex.append('0');
            }
            hex.append(item);
        }
        return hex.toString();
    }

    /**
     * 获取 SDK 参与签名的请求路径。
     *
     * @param requestUrl 完整请求地址
     * @return 请求路径
     */
    private String getUrlPath(String requestUrl) {
        if (requestUrl.startsWith("http")) {
            int pathIndex = requestUrl.indexOf('/', 10);
            if (pathIndex > -1) {
                return requestUrl.substring(pathIndex);
            }
        }
        return requestUrl;
    }

    /**
     * 校验签名所需配置。
     *
     * @param value 配置值
     * @param name  配置名称
     * @return 原配置值
     */
    private String requireConfig(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("云星空 Web API 配置 " + name + " 不能为空");
        }
        return value;
    }

}
