package com.rain;

import com.kingdee.bos.webapi.autoconfigure.EnableK3CloudWebApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * K3 Cloud Open API Starter 联调应用入口。
 *
 * @author xueyu
 * @since 3.0.0
 */
@EnableK3CloudWebApi
@SpringBootApplication
public class K3CloudOpenApiStarterTestApplication {

    /**
     * 启动 Starter 联调应用。
     *
     * @param args 启动参数
     */
    static void main(String[] args) {
        SpringApplication.run(K3CloudOpenApiStarterTestApplication.class, args);
    }
}
