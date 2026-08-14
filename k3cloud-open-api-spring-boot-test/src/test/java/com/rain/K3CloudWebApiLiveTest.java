package com.rain;

import com.alibaba.fastjson2.JSON;
import com.kingdee.bos.webapi.common.utils.api.http.SessionWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.http.SignedWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.sdk.WebApiHelper;
import com.kingdee.bos.webapi.domain.dto.response.result.LoginResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用目标金蝶环境验证 Starter 注册的三条调用路径。
 * <p>
 * 测试内容只包含登录和查询，不会新增或修改业务数据。执行测试前需要在
 * {@code application.yml} 中配置可用的金蝶环境。
 * </p>
 */
@Slf4j
@SpringBootTest(classes = K3CloudOpenApiStarterTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class K3CloudWebApiLiveTest {

    private static final String CURRENCY_QUERY = """
            {"FormId":"BD_Currency","FieldKeys":"FCODE","FilterString":"FNUMBER='PRE001'","Limit":1}
            """;

    private final WebApiHelper webApiHelper;
    private final SessionWebApiHttpHelper sessionWebApiHttpHelper;
    private final SignedWebApiHttpHelper signedWebApiHttpHelper;

    @Autowired
    K3CloudWebApiLiveTest(WebApiHelper webApiHelper,
                          SessionWebApiHttpHelper sessionWebApiHttpHelper,
                          SignedWebApiHttpHelper signedWebApiHttpHelper) {
        this.webApiHelper = webApiHelper;
        this.sessionWebApiHttpHelper = sessionWebApiHttpHelper;
        this.signedWebApiHttpHelper = signedWebApiHttpHelper;
    }

    @Test
    void shouldLoginThroughSessionClient() {
        log.info("开始验证 Session 客户端登录");
        long startTime = System.nanoTime();
        LoginResult loginResult = sessionWebApiHttpHelper.loginBySign();
        long elapsedMillis = elapsedMillis(startTime);

        log.info(
                "Session 客户端登录完成：success={}, loginResultType={}, messageCode={}, message={}, " +
                        "isSuccessByAPI={}, lcid={}, formId={}, hasContext={}, hasSessionId={}, " +
                        "hasAccessToken={}, errorStackTrace={}, elapsedMs={}",
                loginResult.isLoginSuccess(),
                loginResult.getLoginResultType(),
                loginResult.getMessageCode(),
                loginResult.getMessage(),
                loginResult.getIsSuccessByAPI(),
                loginResult.getLcid(),
                loginResult.getFormId(),
                loginResult.getContext() != null,
                loginResult.getKdsvcSessionId() != null && !loginResult.getKdsvcSessionId().isBlank(),
                loginResult.getAccessToken() != null && !loginResult.getAccessToken().isBlank(),
                loginResult.getErrorStackTrace(),
                elapsedMillis);

        assertThat(loginResult.isLoginSuccess()).isTrue();
        assertThat(loginResult.getKdsvcSessionId()).isNotBlank();
    }

    @Test
    void shouldQueryThroughSdkClient() {
        log.info("开始通过 SDK WebApiHelper 执行查询，请求：{}", CURRENCY_QUERY);
        long startTime = System.nanoTime();
        List<List<Object>> result = webApiHelper.executeBillQuery(CURRENCY_QUERY);
        logQueryResult("SDK WebApiHelper", result, elapsedMillis(startTime));

        assertThat(result).isNotNull();
    }

    @Test
    void shouldQueryThroughSessionClient() {
        log.info("开始通过 Session 客户端执行查询，请求：{}", CURRENCY_QUERY);
        long startTime = System.nanoTime();
        List<List<Object>> result = sessionWebApiHttpHelper.executeBillQuery(CURRENCY_QUERY);
        logQueryResult("SessionWebApiHttpHelper", result, elapsedMillis(startTime));

        assertThat(result).isNotNull();
    }

    @Test
    void shouldQueryThroughSignedClient() {
        log.info("开始通过 Signed 客户端执行查询，请求：{}", CURRENCY_QUERY);
        long startTime = System.nanoTime();
        List<List<Object>> result = signedWebApiHttpHelper.executeBillQuery(CURRENCY_QUERY);
        logQueryResult("SignedWebApiHttpHelper", result, elapsedMillis(startTime));

        assertThat(result).isNotNull();
    }

    private void logQueryResult(String clientName, List<List<Object>> result, long elapsedMillis) {
        int rowCount = result == null ? 0 : result.size();
        log.info(
                "{} 查询完成：rowCount={}, elapsedMs={}, result={}",
                clientName,
                rowCount,
                elapsedMillis,
                JSON.toJSONString(result));
    }

    private long elapsedMillis(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000L;
    }
}
