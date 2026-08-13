# K3 Cloud Open API

金蝶云星空 WebAPI 的 Java 封装，提供请求与响应模型、常用接口调用工具，以及 Spring Boot 自动配置 Starter。

## 模块说明

| 模块                                   | 说明                                                                     |
|----------------------------------------|--------------------------------------------------------------------------|
| `k3cloud-open-api-domain`              | 保存、查询、提交、审核等接口的请求与响应模型                             |
| `k3cloud-open-api-common`              | WebAPI 调用工具、HTTP 客户端、异常及 FastJSON2、Gson、Jackson 响应转换器 |
| `k3cloud-open-api-spring-boot-starter` | 配置属性绑定及 Spring Boot 自动配置                                      |

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

该命令会构建全部模块，并将当前版本安装到本地 Maven 仓库。本文示例使用版本 `2.0.0`。

## Spring Boot 快速开始

### 1. 添加依赖

```xml

<dependency>
    <groupId>com.xy</groupId>
    <artifactId>k3cloud-open-api-spring-boot-starter</artifactId>
    <version>2.0.0</version>
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
| `proxy`             | 否   | 无      | 传递给金蝶 SDK 的代理配置，具体格式以 SDK 8.2.0 要求为准；空字符串按未配置处理                     |
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

    public static void main(String[] args) {
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
- `WebApiHttpHelper`：基于 Apache HttpClient 5 的调用工具，容器关闭时自动释放连接资源。

推荐使用构造器注入：

```java
package com.example.material;

import com.kingdee.bos.webapi.common.utils.WebApiHelper;
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

## 自动配置与扩展

配置由 `@EnableK3CloudWebApi` 显式导入。仅当业务项目添加该注解且已引入金蝶云星空 SDK、类路径中存在 `K3CloudApi` 时启用；
仅引入 Starter 不会加载配置。

四个默认 Bean 分别按其返回类型使用 `@ConditionalOnMissingBean`。业务项目声明同类型 Bean 后，对应的默认 Bean 会退让，不依赖
Bean 名。若只声明自定义 `K3CloudApi`，默认 `WebApiProperties`、`WebApiHttpHelper` 和 `WebApiHelper` 仍会按各自条件创建；此时创建默认
`K3CloudApi` 所需的四项必填配置不再校验，但默认 `WebApiHttpHelper` 仍需要完整、有效的连接参数才能正常调用。

业务项目可以声明同类型 Bean 覆盖默认实现。例如，使用自定义 Jackson `ObjectMapper` 创建 `WebApiHelper`：

```java
package com.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingdee.bos.webapi.common.convert.jackson.JacksonConvertApiResponse;
import com.kingdee.bos.webapi.common.utils.WebApiHelper;
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

默认的 `WebApiHelper` 和 `WebApiHttpHelper` 使用 FastJSON2 响应转换器；common 模块同时提供 Gson 和 Jackson 实现供调用方显式选择。

## 注意事项

- 调用前需要在金蝶云星空中创建第三方应用并授予相应接口权限。
- 生产环境应使用 HTTPS、遵循最小权限原则，并建立应用密钥轮换机制。
- `CfgUtilExt` 会设置当前 JVM 内金蝶 SDK 的全局配置；同一应用进程应避免同时配置多个账套客户端。
- `WebApiHttpHelper` 由 Spring 容器管理时会自动关闭；手动创建时应由调用方负责关闭。
- `print-execute-url` 仅控制执行地址日志，不应在日志中输出应用密钥、SessionId 或完整敏感请求内容。
- 项目测试不会访问真实金蝶服务；实际网络连通性、账号权限和业务数据仍需在目标环境验证。

## 版权与许可证

Copyright (c) 2026 xueyu

本项目基于[木兰宽松许可证，第 2 版](LICENSE)发布。
