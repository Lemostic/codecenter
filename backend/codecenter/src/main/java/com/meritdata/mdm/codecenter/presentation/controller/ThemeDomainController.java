package com.meritdata.mdm.codecenter.presentation.controller;

import com.meritdata.mdm.codecenter.application.dto.ThemeDomainRequest;
import com.meritdata.mdm.codecenter.application.service.ThemeDomainService;
import com.meritdata.mdm.codecenter.common.api.ApiResponse;
import com.meritdata.mdm.codecenter.domain.entity.ThemeDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 主题域 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mdm/encode/themes")
@RequiredArgsConstructor
public class ThemeDomainController {

    private final ThemeDomainService themeDomainService;

    @PostMapping
    public ApiResponse<ThemeDomain> create(@RequestBody ThemeDomainRequest req,
                                           @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Theme created", themeDomainService.create(req, operatorId));
    }

    @PutMapping("/{id}")
    public ApiResponse<ThemeDomain> update(@PathVariable String id, @RequestBody ThemeDomainRequest req,
                                           @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Theme updated", themeDomainService.update(id, req, operatorId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        themeDomainService.delete(id, operatorId);
        return ApiResponse.ok("Theme deleted", null);
    }

    @GetMapping("/tree")
    public ApiResponse<List<ThemeDomain>> tree(@RequestParam(required = false) String tenantId) {
        return ApiResponse.ok(themeDomainService.tree(tenantId == null ? "default" : tenantId));
    }

    @GetMapping("/children")
    public ApiResponse<List<ThemeDomain>> children(@RequestParam(required = false) String tenantId,
                                                   @RequestParam(required = false) String parentId) {
        return ApiResponse.ok(themeDomainService.children(tenantId == null ? "default" : tenantId, parentId));
    }
}
