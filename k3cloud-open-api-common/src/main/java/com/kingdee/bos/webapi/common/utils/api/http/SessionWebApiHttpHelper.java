package com.kingdee.bos.webapi.common.utils.api.http;

import com.alibaba.fastjson2.JSONPath;
import com.kingdee.bos.webapi.common.convert.WebApiResponseConverter;
import com.kingdee.bos.webapi.common.convert.fastjson.FastJsonConvertApiResponse;
import com.kingdee.bos.webapi.common.enums.WebApiService;
import com.kingdee.bos.webapi.common.exception.WebApiInvokeException;
import com.kingdee.bos.webapi.config.properties.WebApiProperties;
import com.kingdee.bos.webapi.domain.dto.response.result.LoginResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * 使用登录会话认证调用金蝶 K3Cloud Web API 的 HTTP 辅助类。
 * <p>
 * 本类负责 {@code LoginBySign} 登录、SessionId 注入和会话有效性检查，公共 HTTP
 * 执行与资源生命周期由 {@link AbstractWebApiHttpHelper} 管理。
 * </p>
 *
 * @author xueyu
 * @see AbstractWebApiHttpHelper
 * @since 3.0.0
 */
@Slf4j
public class SessionWebApiHttpHelper extends AbstractWebApiHttpHelper {

    /**
     * 会话检查的最小时间间隔，单位为毫秒。
     * 该常量定义了在会话保持活跃状态检查中，两次检查之间的最短等待时间。
     * 主要用于控制会话心跳检测的频率，以避免过于频繁的检查对系统或服务端造成不必要的负载。
     * 例如，设置为30000毫秒表示每30秒最多进行一次会话有效性检查。
     * 该间隔时间应结合具体业务场景和性能要求进行配置，确保在维持会话有效性的同时，不过度消耗资源。
     */
    private static final long SESSION_CHECK_INTERVAL_MS = 30_000;
    /**
     * 用于存储当前登录结果，包括会话ID等信息。
     * 使用 volatile 关键字确保多线程环境下对 loginResult 的可见性。
     */
    private volatile LoginResult loginResult;
    /**
     * 记录最近一次成功校验会话有效性的时间戳。
     * 该字段用于控制会话校验的频率，避免过于频繁地发送心跳请求。
     * 当距离上次成功校验的时间超过预定的间隔（由 SESSION_CHECK_INTERVAL_MS 定义）时，
     * 才会再次执行会话有效性检查。
     * 此字段声明为 volatile，以确保在多线程环境下其值变更的可见性。
     */
    private volatile long lastSessionCheckTime = 0L;

    /**
     * 私有构造函数，用于创建 WebApiHttpHelper 的实例。
     * 通过该构造函数，将 WebApiProperties 和 WebApiResponseConverter 注入到当前类中，
     * 确保类的依赖项在实例化时被正确初始化。
     *
     * @param webApiProperties        包含与金蝶 K3Cloud Web API 交互所需的配置信息，例如服务地址、账套ID、用户凭据等。
     *                                该参数不能为空，且其内容通常通过配置文件或外部化方式进行管理。
     * @param webApiResponseConverter 用于解析 API 响应字符串的转换器实例。
     *                                该参数提供了将 API 响应解析为特定业务对象的能力，支持多种业务场景的响应处理。
     */
    private SessionWebApiHttpHelper(WebApiProperties webApiProperties, WebApiResponseConverter webApiResponseConverter) {
        super(webApiProperties, webApiResponseConverter);
    }

    /**
     * 创建一个 WebApiHttpHelper 实例，用于处理金蝶 K3Cloud Web API 的请求和响应。
     * 该方法通过指定的 WebApiProperties 配置信息进行初始化，并使用默认的 FastJsonConvertApiResponse 作为响应解析器。
     * 确保在与 Web API 交互时具备必要的配置支持和响应解析能力。
     *
     * @param webApiProperties 包含与金蝶 K3Cloud Web API 交互所需的配置信息，例如服务地址、账套ID、用户凭据等。
     *                         该参数不能为空，且其内容通常通过配置文件或外部化方式进行管理。
     * @return 返回一个初始化完成的 WebApiHttpHelper 实例，用于处理 Web API 请求和响应。
     */
    public static SessionWebApiHttpHelper of(final WebApiProperties webApiProperties) {
        return of(webApiProperties, FastJsonConvertApiResponse.INSTANCE);
    }

    /**
     * 创建一个 WebApiHttpHelper 实例，用于处理金蝶 K3Cloud Web API 的请求和响应。
     * 该方法通过指定的 WebApiProperties 和 WebApiResponseConverter 实例进行初始化，
     * 确保在与 Web API 交互时能够正确配置和解析响应消息。
     *
     * @param webApiProperties   包含与金蝶 K3Cloud Web API 交互所需的配置信息，例如服务地址、账套ID、用户凭据等。
     *                           该参数不能为空，且其内容通常通过配置文件或外部化方式进行管理。
     * @param convertApiResponse 用于解析 API 响应字符串的转换器实例。
     *                           该参数提供了将 API 响应解析为特定业务对象的能力，支持多种业务场景的响应处理。
     *                           该参数不能为空。
     * @return 返回一个 WebApiHttpHelper 实例，用于处理金蝶 K3Cloud Web API 的请求和响应。
     */
    public static SessionWebApiHttpHelper of(WebApiProperties webApiProperties, WebApiResponseConverter convertApiResponse) {
        if (Objects.isNull(webApiProperties)) {
            throw new NullPointerException("云星空WebApi客户端不能为空!");
        }
        if (Objects.isNull(convertApiResponse)) {
            throw new NullPointerException("响应消息转换插件不能为空!");
        }
        return new SessionWebApiHttpHelper(webApiProperties, convertApiResponse);
    }

    /**
     * 构建 LoginBySign 方法所需的参数数组。
     * 该方法根据 WebApiProperties 中配置的账套ID、用户名、应用ID、应用秘钥和语言ID，
     * 并生成时间戳和签名，最终返回一个包含这些信息的 Object 数组。
     *
     * @return 包含 LoginBySign 所需参数的 Object 数组。
     */
    private Object[] buildLoginBySignParams() {
        String acctId = webApiProperties.getAcctId();
        String userName = webApiProperties.getUserName();
        String appId = webApiProperties.getAppId();
        String appSec = webApiProperties.getAppSec();
        int lcId = webApiProperties.getLcId();
        // 生成时间戳（秒）
        long timestamp = System.currentTimeMillis() / 1000;
        // 生成签名
        String sign = generateSign(acctId, userName, appId, appSec, timestamp);
        // 参数依次为账套ID、用户名、应用ID、时间戳、签名信息、语言ID
        return new Object[]{acctId, userName, appId, timestamp, sign, lcId};
    }

    /**
     * 生成 SHA-256 签名字符串。
     * 该方法将账套ID、用户名、应用ID、应用秘钥和时间戳进行排序，
     * 然后使用 SHA-256 算法进行加密，最终返回十六进制表示的签名字符串。
     *
     * @param acctId    账套ID。
     * @param userName  用户名。
     * @param appId     应用ID。
     * @param appSecret 应用秘钥。
     * @param timestamp 时间戳（秒）。
     * @return 生成的 SHA-256 签名字符串。
     * @throws WebApiInvokeException 如果生成签名失败（例如，不支持 SHA-256 算法）。
     */
    private String generateSign(String acctId, String userName, String appId, String appSecret, long timestamp) {
        // 参数非空校验，防止出现隐式 NPE 或不确定签名结果
        if (acctId == null || userName == null || appId == null || appSecret == null) {
            throw new IllegalArgumentException("生成签名的参数不能为空");
        }
        // 将参数转换为字符串数组并排序，保证顺序一致性
        String[] arr = new String[]{
                acctId,
                userName,
                appId,
                appSecret,
                String.valueOf(timestamp)
        };
        Arrays.sort(arr);
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            for (String str : arr) {
                sha256.update(str.getBytes(StandardCharsets.UTF_8));
            }
            byte[] hashBytes = sha256.digest();
            // 转换为十六进制字符串
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                String s = Integer.toHexString(0xff & b);
                if (s.length() == 1) {
                    hex.append('0');
                }
                hex.append(s);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new WebApiInvokeException("生成签名失败：不支持 SHA-256 算法", e);
        }
    }

    /**
     * 执行金蝶 K3Cloud Web API 的 LoginBySign 登录操作。
     * 该方法构建登录请求，发送到 API 服务器，并解析登录响应。
     * 在登录成功后，会更新 {@code loginResult} 成员变量，以便后续请求使用会话ID。
     * 如果登录失败或发生 IO 异常，{@code loginResult} 将被置为 null。
     *
     * @return 包含登录结果的 {@code LoginResult} 对象。
     * @throws WebApiInvokeException 如果 LoginBySign 调用失败。
     */
    public LoginResult loginBySign() {
        try {
            String response = executeRaw(
                    WebApiService.LOGIN_BY_SIGN.getServiceName(),
                    buildLoginBySignParams());
            if (log.isDebugEnabled()) {
                log.debug("登录响应: {}", response);
            }
            LoginResult result = webApiResponseConverter.parseLoginResponse(response);
            // 更新登录结果
            this.loginResult = result;
            return result;
        } catch (WebApiInvokeException e) {
            this.loginResult = null;
            throw e;
        } catch (Exception e) {
            // 在登录失败时将 loginResult 置为 null
            this.loginResult = null;
            throw new WebApiInvokeException("LoginBySign failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeExecute() {
        ensureLogin();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void customizeRequest(HttpPost request, String requestUrl) {
        if (loginResult != null && loginResult.isLoginSuccess() && loginResult.getKdsvcSessionId() != null) {
            request.setHeader("kdservice-sessionid", loginResult.getKdsvcSessionId());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void clearAuthenticationState() {
        loginResult = null;
        lastSessionCheckTime = 0L;
    }

    /**
     * 确保当前会话处于有效的登录状态。
     * 该方法首先检查当前登录结果是否有效且会话未过期，若满足条件则直接返回。
     * 若登录状态无效或会话已过期，则进入同步块进行双重检查，避免多线程环境下的重复登录。
     * 在同步块内，若登录状态仍未满足条件，则尝试调用登录方法进行重新登录。
     * 若重新登录失败，将抛出 WebApiInvokeException 异常。
     * 若重新登录成功，则更新登录结果并记录新的会话ID。
     * 此方法主要用于在执行需要认证的API请求前，自动维护登录会话的有效性。
     */
    private void ensureLogin() {
        if (loginResult != null && loginResult.isLoginSuccess() && isSessionStillValid()) {
            return;
        }
        synchronized (this) {
            if (loginResult != null && loginResult.isLoginSuccess() && isSessionStillValid()) {
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("当前未登录或登录已失效，尝试重新登录...");
            }
            this.loginResult = null;
            LoginResult reloginResult = loginBySign();
            if (!reloginResult.isLoginSuccess()) {
                throw new WebApiInvokeException("重新登录失败!");
            }
            if (log.isDebugEnabled()) {
                log.debug("重新登录成功，SessionId: {}", reloginResult.getKdsvcSessionId());
            }
        }
    }

    /**
     * 检查当前会话是否仍然有效。
     * 该方法通过比较当前时间与上次会话检查时间的时间差，来决定是否需要执行内部有效性检查。
     * 如果时间差小于预设的会话检查间隔，则直接返回true，避免频繁进行内部检查。
     * 否则，调用内部方法进行实际的有效性验证，并在验证通过时更新上次会话检查时间。
     *
     * @return 如果会话仍然有效则返回true，否则返回false。
     */
    private boolean isSessionStillValid() {
        long now = System.currentTimeMillis();
        if (now - lastSessionCheckTime < SESSION_CHECK_INTERVAL_MS) {
            return true;
        }
        boolean validInternal = isSessionStillValidInternal();
        if (validInternal) {
            lastSessionCheckTime = now;
        }
        return validInternal;
    }

    /**
     * 内部方法，用于检查当前会话是否仍然有效。
     * 通过发送一个预定义的心跳查询请求到金蝶K3Cloud Web API，并根据响应内容判断会话状态。
     * 心跳查询请求固定查询币别表单中编码为'PRE001'的记录，并期望返回包含"[CNY]"的响应。
     * 若响应中包含"[CNY]"，则认为会话有效；否则，尝试从响应中解析MsgCode字段。
     * 当MsgCode等于1时，表示未登录或会话已失效；其他情况则认为会话仍然有效。
     * 若在检查过程中发生任何异常，将会话视为无效，并记录错误日志。
     *
     * @return 如果会话仍然有效则返回true，否则返回false。
     */
    private boolean isSessionStillValidInternal() {
        try {
            String heartbeat = "{\"FormId\":\"BD_Currency\",\"FieldKeys\":\"FCODE\",\"OrderString\":\"\",\"FilterString\":\" FNUMBER='PRE001' \",\"TopRowCount\":\"0\",\"StartRow\":\"0\",\"Limit\":\"0\"}";
            if (webApiProperties.isPrintExecuteUrl()) {
                log.info("Kingdee K3Cloud Web API Heartbeat Check");
            }
            String resp = executeRaw(WebApiService.EXECUTE_BILL_QUERY.getServiceName(), new Object[]{heartbeat});
            if (resp.contains("[CNY]")) {
                //情况1：正常业务返回 [["CNY"]] 或类似二维数组，认为 session 有效
                return true;
            }
            // 情况2：尝试解析 MsgCode
            Object val = JSONPath.eval(resp, "$[0][0].Result.ResponseStatus.MsgCode");
            Integer msgCode = null;
            if (val instanceof Number number) {
                msgCode = number.intValue();
            } else if (val instanceof String string) {
                msgCode = Integer.valueOf(string);
            }
            // MsgCode == 1 => 未登录 / 会话失效
            return !Objects.equals(msgCode, 1);
        } catch (Exception e) {
            log.error("Session 校验异常，视为无效，将触发重登", e);
            return false;
        }
    }


}
