package com.rain;

import com.kingdee.bos.webapi.common.utils.api.http.SessionWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.http.SignedWebApiHttpHelper;
import com.kingdee.bos.webapi.common.utils.api.sdk.WebApiHelper;
import com.kingdee.bos.webapi.domain.dto.response.result.LoginResult;
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
        LoginResult loginResult = sessionWebApiHttpHelper.loginBySign();

        assertThat(loginResult.isLoginSuccess()).isTrue();
        assertThat(loginResult.getKdsvcSessionId()).isNotBlank();
    }

    @Test
    void shouldQueryThroughSdkClient() {
        List<List<Object>> result = webApiHelper.executeBillQuery(CURRENCY_QUERY);

        assertThat(result).isNotNull();
    }

    @Test
    void shouldQueryThroughSessionClient() {
        List<List<Object>> result = sessionWebApiHttpHelper.executeBillQuery(CURRENCY_QUERY);

        assertThat(result).isNotNull();
    }

    @Test
    void shouldQueryThroughSignedClient() {
        List<List<Object>> result = signedWebApiHttpHelper.executeBillQuery(CURRENCY_QUERY);

        assertThat(result).isNotNull();
    }
}
