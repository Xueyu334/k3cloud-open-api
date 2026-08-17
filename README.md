# K3 Cloud Open API

金蝶云星空 WebAPI 的 Java 封装，提供请求与响应模型、常用接口调用工具，以及 Spring Boot 自动配置 Starter。

## 模块说明

| 模块                                   | 说明                                                                     |
|----------------------------------------|--------------------------------------------------------------------------|
| `k3cloud-open-api-domain`              | 保存、查询、提交、审核等接口的请求与响应模型                             |
| `k3cloud-open-api-common`              | WebAPI 调用工具、会话/签名 HTTP 客户端、异常及多种响应转换器             |
| `k3cloud-open-api-spring-boot-starter` | 配置属性绑定及 Spring Boot 自动配置                                      |
| `k3cloud-open-api-spring-boot-test`    | Starter 的可启动联调项目、上下文测试及真实接口测试                       |

## 兼容基线

- 本仓库使用 JDK 25、Maven 3.9 和 Spring Boot 4.1.0 构建并验证。
- Starter 面向 Spring Boot 4.1.0；其他 Spring Boot 版本尚未验证。

## 接入前提

- 业务应用能够访问金蝶云星空服务。
- 已创建第三方应用，并获得账套 ID、应用 ID 和应用密钥。
- 第三方应用及授权用户具有目标表单和接口权限。

## 构建

在项目根目录执行：

```shell
mvn clean install
```

该命令会构建全部模块，并将当前版本安装到本地 Maven 仓库。本文示例使用版本 `3.0.0`。

### 发布到私有 Maven 仓库

发布范围包括以下三个可复用模块：

- `k3cloud-open-api-domain`
- `k3cloud-open-api-common`
- `k3cloud-open-api-spring-boot-starter`

每个模块都会发布 POM、主 JAR 和 `-sources.jar` 源码包。父工程 `k3cloud-open-api` 仅参与聚合构建，
`k3cloud-open-api-spring-boot-test` 仅用于联调，两者均配置为跳过发布。

#### 1. 配置发布仓库

父 `pom.xml` 通过 `distributionManagement` 指定发布目标，仓库 `id` 同时用于匹配 Maven 本机凭据：

```xml
<distributionManagement>
    <repository>
        <id>2488063-release-YmmVR6</id>
        <url>https://packages.aliyun.com/66c2c0df684fca2ef650a01a/maven/2488063-release-ymmvr6</url>
    </repository>
</distributionManagement>
```

项目通过 `maven-source-plugin` 附加源码 JAR；通过 `flatten-maven-plugin` 生成不依赖父工程的消费端 POM，
并将继承配置及依赖版本解析为固定值。父工程的 `maven-deploy-plugin` 使用 `skip=true` 且不向子模块继承，
因此父工程仍可参与 reactor 构建，但不会上传到私库。

#### 2. 配置本机凭据

在 Maven 用户配置文件中添加与 `distributionManagement.repository.id` 完全相同的 `server`。Windows 默认路径为
`C:\Users\<用户名>\.m2\settings.xml`，Linux 和 macOS 默认路径为 `~/.m2/settings.xml`：

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
    <servers>
        <server>
            <id>2488063-release-YmmVR6</id>
            <username>替换为私库用户名</username>
            <password>替换为私库密码或访问令牌</password>
        </server>
    </servers>
</settings>
```

`settings.xml` 属于开发机或 CI 环境的私有配置，不要将真实用户名、密码或访问令牌提交到仓库。

#### 3. 修改发布版本

发布前修改父 `pom.xml` 中的 `revision`。正式版本应使用未发布过的新版本号，避免覆盖已有制品：

```xml
<properties>
    <revision>3.0.0</revision>
</properties>
```

三个子模块继承该版本，模块间依赖也会由扁平化 POM 解析为相同的固定版本。

#### 4. 执行发布

在项目根目录执行：

```shell
mvn clean deploy -pl k3cloud-open-api-domain,k3cloud-open-api-common,k3cloud-open-api-spring-boot-starter -am -DskipTests
```

- `-pl` 只选择三个需要发布的模块。
- `-am` 同时构建这些模块在当前 reactor 中依赖的项目。
- `deploy` 执行编译、打包、本地安装和私库上传。
- `-DskipTests` 跳过测试执行；正式发布前需要执行测试时可移除此参数。

发布成功后，日志中父工程会显示 `Skipping artifact deployment`，三个目标模块会显示 POM、主 JAR 和源码 JAR 的
`Uploaded` 记录，并以 `BUILD SUCCESS` 结束。

### Starter 联调模块

执行 Starter 上下文测试及真实接口测试：

```shell
mvn -pl k3cloud-open-api-spring-boot-test -am test
```

启动联调应用前，在测试模块的 `application.yml` 中直接填写目标环境的连接及认证信息；配置项与下文 Spring Boot 快速开始保持一致。
配置文件默认使用 `http://localhost/` 和测试占位凭据，因此未修改时只会验证应用和 Bean 能否正常启动，不会主动调用接口。

```shell
mvn -pl k3cloud-open-api-spring-boot-test -am package -DskipTests
java -jar k3cloud-open-api-spring-boot-test/target/k3cloud-open-api-spring-boot-test-3.0.0.jar
```

`K3CloudWebApiLiveTest` 会随 Maven 测试默认执行，使用 `application.yml` 中的配置验证 SDK、Session 和 Signed 三条只读查询路径。
执行前必须把测试占位配置替换为可用的金蝶环境配置：

```powershell
mvn -pl k3cloud-open-api-spring-boot-test -am test `
  -Dtest=K3CloudWebApiLiveTest `
  -Dsurefire.failIfNoSpecifiedTests=false
```

不要将上述真实配置写入或提交到仓库。

## Spring Boot 快速开始

### 1. 添加依赖

```xml

<dependency>
    <groupId>com.xy</groupId>
    <artifactId>k3cloud-open-api-spring-boot-starter</artifactId>
    <version>3.0.0</version>
</dependency>

<dependency>
    <groupId>com.kingdee</groupId>
    <artifactId>k3cloud-webapi-sdk-jdk11</artifactId>
    <version>8.2.0</version>
</dependency>
```

Starter 会传递引入 `common` 和 `domain`，但不会传递金蝶云星空 SDK。业务项目需要单独声明与本项目兼容的 SDK 8.2.0 依赖。

### 2. 配置连接参数

在业务应用的 `application.yml` 中配置：

```yaml
kingdee:
  k3cloud:
    web-api:
      server-url: ${K3CLOUD_SERVER_URL}
      acct-id: ${K3CLOUD_ACCT_ID}
      user-name: ${K3CLOUD_USER_NAME:}
      app-id: ${K3CLOUD_APP_ID}
      app-sec: ${K3CLOUD_APP_SECRET}
      lc-id: 2052
      org-num: ${K3CLOUD_ORG_NUM:}
      connect-timeout: 120
      request-timeout: 120
      stock-timeout: 180
      proxy: ${K3CLOUD_PROXY:}
      print-execute-url: false
```

请通过环境变量或密钥管理服务提供账套及应用凭据，不要把真实凭据提交到代码仓库。

| 配置项              | 必填 | 默认值  | 说明                                                                                               |
|---------------------|------|---------|----------------------------------------------------------------------------------------------------|
| `server-url`        | 是   | 无      | 包含协议和 K3Cloud 上下文路径的服务地址，例如 `https://host.example.com/k3cloud/`；末尾 `/` 可省略 |
| `acct-id`           | 是   | 无      | 账套 ID                                                                                            |
| `user-name`         | 否   | 无      | 第三方应用授权用户                                                                                 |
| `app-id`            | 是   | 无      | 第三方应用 ID                                                                                      |
| `app-sec`           | 是   | 无      | 第三方应用密钥                                                                                     |
| `lc-id`             | 否   | `2052`  | 账套语系，`2052` 表示简体中文                                                                      |
| `org-num`           | 否   | 无      | 多组织场景使用的组织编码                                                                           |
| `connect-timeout`   | 否   | `120`   | 连接超时，单位为秒                                                                                 |
| `request-timeout`   | 否   | `120`   | 请求超时，单位为秒                                                                                 |
| `stock-timeout`     | 否   | `180`   | 套接字超时，单位为秒；`stock` 是当前 Java 属性的兼容命名                                           |
| `proxy`             | 否   | 无      | 传递给金蝶 SDK 的全局代理配置，具体格式以 SDK 8.2.0 要求为准；空字符串按未配置处理                 |
| `print-execute-url` | 否   | `false` | 是否记录 WebAPI 执行地址                                                                           |

`server-url`、`acct-id`、`app-id` 或 `app-sec` 缺失时，应用会在创建默认客户端 Bean 时快速失败。

### 3. 启用 Web API

在 Spring Boot 应用启动类或配置类上添加 `@EnableK3CloudWebApi`：

```java
package com.example;

import com.kingdee.bos.webapi.autoconfigure.EnableK3CloudWebApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableK3CloudWebApi
@SpringBootApplication
public class Application {

    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

仅引入 Starter 不会加载金蝶云星空 Web API 配置，也不会创建相关 Bean。

### 4. 注入并调用

启用后，Starter 默认注册以下 Bean：

- `WebApiProperties`：绑定 `kingdee.k3cloud.web-api` 配置。
- `K3CloudApi`：金蝶 SDK 客户端，Bean 名为 `k3CloudApiClient`。
- `WebApiHelper`：基于金蝶 SDK 的常用接口封装。
- `SessionWebApiHttpHelper`：使用 `LoginBySign` 自动登录并维护会话的 HTTP 客户端。
- `SignedWebApiHttpHelper`：参照 SDK `ApiRequester#buildHeader` 为每次请求生成签名的 HTTP 客户端。

推荐使用构造器注入：

```java
package com.example.material;

import com.kingdee.bos.webapi.common.utils.api.sdk.WebApiHelper;
import com.kingdee.bos.webapi.domain.dto.request.save.ModelMap;
import com.kingdee.bos.webapi.domain.dto.request.save.SaveRequest;
import com.kingdee.bos.webapi.domain.dto.response.WebApiResp;
import com.kingdee.bos.webapi.domain.dto.response.result.SaveResult;
import org.springframework.stereotype.Service;

@Service
public class MaterialService {

    private final WebApiHelper webApiHelper;

    public MaterialService(WebApiHelper webApiHelper) {
        this.webApiHelper = webApiHelper;
    }

    public WebApiResp<SaveResult> saveMaterial(String number, String name) {
        ModelMap<String, Object> model = ModelMap.<String, Object>of()
                .with("FNumber", number)
                .with("FName", name);

        SaveRequest request = new SaveRequest("BD_MATERIAL");
        request.setModel(model);

        WebApiResp<SaveResult> response = webApiHelper.saveResult(request);
        if (!response.isSuccessfully()) {
            throw new IllegalStateException(response.getErrorMessage());
        }
        return response;
    }
}
```

示例中的表单标识和字段仅用于展示调用方式。实际保存字段、必填项和字段类型应以目标账套的表单元数据为准。

### 5. 选择 HTTP 认证方式

Starter 会同时注册会话认证和逐请求签名两个 HTTP 客户端，业务代码可以按具体类型注入需要的实现：

| 实现                         | 认证方式                 | 适用场景                                                   |
|------------------------------|--------------------------|------------------------------------------------------------|
| `SessionWebApiHttpHelper`    | 自动登录并复用 SessionId | 需要兼容传统登录会话、连续执行多次请求                     |
| `SignedWebApiHttpHelper`     | 每次请求生成签名请求头   | 希望避免维护登录状态，直接采用 SDK `buildHeader` 认证规则  |

两个实现均继承 `AbstractWebApiHttpHelper`，共享连接池、Cookie、请求执行、响应转换和资源关闭逻辑，并提供
`execute`、`save`、`executeBillQuery` 等便捷方法。例如使用逐请求签名客户端保存单据：

```java
package com.example.material;

import com.kingdee.bos.webapi.common.utils.api.http.SignedWebApiHttpHelper;
import com.kingdee.bos.webapi.domain.dto.request.save.SaveRequest;
import com.kingdee.bos.webapi.domain.dto.response.WebApiResp;
import com.kingdee.bos.webapi.domain.dto.response.result.SaveResult;
import org.springframework.stereotype.Service;

@Service
public class SignedMaterialService {

    private final SignedWebApiHttpHelper webApiHttpHelper;

    public SignedMaterialService(SignedWebApiHttpHelper webApiHttpHelper) {
        this.webApiHttpHelper = webApiHttpHelper;
    }

    public WebApiResp<SaveResult> save(String formId, SaveRequest request) {
        return webApiHttpHelper.save(formId, request);
    }
}
```

两种实现遇到 HTTP 状态码大于等于 `400` 时，都会抛出 `WebApiInvokeException`；异常中保留 HTTP 状态码，
异常消息中包含服务端响应内容。金蝶返回 HTTP `200` 但业务响应表示失败时，仍应根据响应对象判断业务结果。

## 自动配置与扩展

配置由 `@EnableK3CloudWebApi` 显式导入。仅当业务项目添加该注解且已引入金蝶云星空 SDK、类路径中存在 `K3CloudApi` 时启用；
仅引入 Starter 不会加载配置。

五个默认 Bean 分别按其返回类型使用 `@ConditionalOnMissingBean`。业务项目声明同类型 Bean 后，对应的默认 Bean 会退让，不依赖
Bean 名。若只声明自定义 `K3CloudApi`，默认 `WebApiProperties`、两个 HTTP Helper 和 `WebApiHelper` 仍会按各自条件创建；此时创建默认
`K3CloudApi` 所需的四项必填配置不再校验，但两个 HTTP Helper 仍需要完整、有效的连接及认证参数才能正常调用。

业务项目可以声明同类型 Bean 覆盖默认实现。例如，使用自定义 Jackson `ObjectMapper` 创建 `WebApiHelper`：

```java
package com.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingdee.bos.webapi.common.convert.jackson.JacksonConvertApiResponse;
import com.kingdee.bos.webapi.common.utils.api.sdk.WebApiHelper;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class K3CloudWebApiConfiguration {

    @Bean
    WebApiHelper webApiHelper(K3CloudApi k3CloudApi, ObjectMapper objectMapper) {
        return WebApiHelper.of(k3CloudApi, new JacksonConvertApiResponse(objectMapper));
    }
}
```

默认的 `WebApiHelper`、`SessionWebApiHttpHelper` 和 `SignedWebApiHttpHelper` 使用 FastJSON2 响应转换器；common 模块同时提供 Gson 和
Jackson 实现供调用方显式选择。

## 3.0.0 升级说明

3.0.0 调整了公共工具类的包结构和 HTTP 客户端抽象，升级时需要同步修改调用方：

- `WebApiHelper` 从 `com.kingdee.bos.webapi.common.utils` 移至 `com.kingdee.bos.webapi.common.utils.api.sdk`。
- 原 `WebApiHttpHelper` 由 `SessionWebApiHttpHelper` 替代，并新增 `SignedWebApiHttpHelper`。
- 两个 HTTP 客户端共同继承 `AbstractWebApiHttpHelper`，业务便捷方法和 HTTP 生命周期由父类统一管理。
- HTTP 状态码大于等于 `400` 时，两个 HTTP 客户端现在统一抛出 `WebApiInvokeException`。

## 注意事项

- 调用前需要在金蝶云星空中创建第三方应用并授予相应接口权限。
- 生产环境应使用 HTTPS、遵循最小权限原则，并建立应用密钥轮换机制。
- `CfgUtilExt` 会设置当前 JVM 内金蝶 SDK 的全局配置，以支持 SDK 通过 `HttpUtils#getProxy()` 获取代理；同一进程中配置多个客户端时，后创建的配置会覆盖先前的全局配置。
- `SessionWebApiHttpHelper` 和 `SignedWebApiHttpHelper` 由 Spring 容器管理时会自动关闭；手动创建时应由调用方负责关闭。
- `print-execute-url` 仅控制执行地址日志，不应在日志中输出应用密钥、SessionId 或完整敏感请求内容。
- 项目测试不会访问真实金蝶服务；实际网络连通性、账号权限和业务数据仍需在目标环境验证。

## 版权与许可证

Copyright (c) 2026 xueyu

本项目基于[木兰宽松许可证，第 2 版](LICENSE)发布。
