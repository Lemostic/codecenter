package com.meritdata.mdm.codecenter.presentation.controller;

import com.meritdata.mdm.codecenter.application.service.ModelPublishService;
import com.meritdata.mdm.codecenter.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查 / 系统信息
 */
@RestController
@RequestMapping("/api/mdm/encode")
@RequiredArgsConstructor
public class HealthController {

    private final ModelPublishService modelPublishService;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "codecenter");
        data.put("version", "0.1.0");
        data.put("dbDialect", modelPublishService.detectDialect());
        data.put("timestamp", System.currentTimeMillis());
        return ApiResponse.ok("Service is up", data);
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "codecenter");
        data.put("description", "Code Center - Master Data Encoding Management");
        data.put("version", "0.1.0");
        data.put("modules", new String[]{
                "模型管理", "模型元数据管理", "编码规则", "码段管理", "模型发布"
        });
        return ApiResponse.ok(data);
    }
}
