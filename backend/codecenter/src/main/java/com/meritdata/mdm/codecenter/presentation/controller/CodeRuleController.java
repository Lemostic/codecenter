package com.meritdata.mdm.codecenter.presentation.controller;

import com.meritdata.mdm.codecenter.application.dto.CodeRuleRequest;
import com.meritdata.mdm.codecenter.application.service.CodeRuleService;
import com.meritdata.mdm.codecenter.common.api.ApiResponse;
import com.meritdata.mdm.codecenter.common.api.PageResponse;
import com.meritdata.mdm.codecenter.domain.entity.CodeRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 编码规则 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mdm/encode/rules")
@RequiredArgsConstructor
public class CodeRuleController {

    private final CodeRuleService codeRuleService;

    @PostMapping
    public ApiResponse<CodeRule> create(@RequestBody CodeRuleRequest req,
                                        @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Rule created", codeRuleService.create(req, operatorId));
    }

    @PutMapping("/{id}")
    public ApiResponse<CodeRule> update(@PathVariable String id, @RequestBody CodeRuleRequest req,
                                        @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Rule updated", codeRuleService.update(id, req, operatorId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        codeRuleService.delete(id, operatorId);
        return ApiResponse.ok("Rule deleted", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<CodeRule> get(@PathVariable String id) {
        return ApiResponse.ok(codeRuleService.getById(id));
    }

    @GetMapping("/list")
    public ApiResponse<PageResponse<CodeRule>> list(@RequestParam String modelId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Page<CodeRule> p = codeRuleService.listByModel(modelId, page, size);
        return ApiResponse.ok(PageResponse.of(p.getContent(), p.getTotalElements(), page, size));
    }

    @GetMapping("/versions")
    public ApiResponse<List<CodeRule>> listVersions(@RequestParam String modelId,
                                                    @RequestParam String fieldId) {
        return ApiResponse.ok(codeRuleService.listVersions(modelId, fieldId));
    }

    @GetMapping("/effective")
    public ApiResponse<CodeRule> getEffective(@RequestParam String modelId,
                                              @RequestParam String fieldId) {
        return ApiResponse.ok(codeRuleService.getEffective(modelId, fieldId));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<CodeRule> publish(@PathVariable String id,
                                         @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Rule published", codeRuleService.publish(id, operatorId));
    }

    @PostMapping("/{id}/revise")
    public ApiResponse<CodeRule> revise(@PathVariable String id,
                                        @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Rule revised", codeRuleService.revise(id, operatorId));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<CodeRule> disable(@PathVariable String id,
                                         @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Rule disabled", codeRuleService.disable(id, operatorId));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<CodeRule> enable(@PathVariable String id,
                                        @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Rule enabled", codeRuleService.enable(id, operatorId));
    }
}
