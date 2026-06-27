package com.meritdata.mdm.codecenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 编码中心 (Code Center) 启动类
 *
 * 主数据平台下的独立微服务，承担:
 *   - 模型管理 (Model Management)
 *   - 模型元数据管理 (Model Metadata)
 *   - 编码规则 (Code Rule)
 *   - 码段管理 (Code Segment, 6 types V0.3)
 *   - 模型发布 (Model Publish)
 */
@SpringBootApplication(exclude = {
        me.ahoo.cosid.spring.boot.starter.actuate.CosIdEndpointAutoConfiguration.class,
        me.ahoo.cosid.spring.boot.starter.mybatis.CosIdMybatisAutoConfiguration.class
})
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
public class CodecenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodecenterApplication.class, args);
    }
}
