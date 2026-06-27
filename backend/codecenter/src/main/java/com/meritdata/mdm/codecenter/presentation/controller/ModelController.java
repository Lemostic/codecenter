package com.meritdata.mdm.codecenter.presentation.controller;

import com.meritdata.mdm.codecenter.application.dto.ModelAttributeRequest;
import com.meritdata.mdm.codecenter.application.dto.ModelRequest;
import com.meritdata.mdm.codecenter.application.service.ModelAttributeService;
import com.meritdata.mdm.codecenter.application.service.ModelPublishService;
import com.meritdata.mdm.codecenter.application.service.ModelService;
import com.meritdata.mdm.codecenter.common.api.ApiResponse;
import com.meritdata.mdm.codecenter.common.api.PageResponse;
import com.meritdata.mdm.codecenter.domain.entity.Model;
import com.meritdata.mdm.codecenter.domain.entity.ModelAttribute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模型 REST 控制器 - 模型管理 + 模型元数据 + 模型发布
 */
@Slf4j
@RestController
@RequestMapping("/api/mdm/encode/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;
    private final ModelAttributeService modelAttributeService;
    private final ModelPublishService modelPublishService;

    @PostMapping
    public ApiResponse<Model> create(@RequestBody ModelRequest req,
                                     @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Model created", modelService.create(req, operatorId));
    }

    @PutMapping("/{id}")
    public ApiResponse<Model> update(@PathVariable String id, @RequestBody ModelRequest req,
                                     @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Model updated", modelService.update(id, req, operatorId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        modelService.delete(id, operatorId);
        return ApiResponse.ok("Model deleted", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<Model> get(@PathVariable String id) {
        return ApiResponse.ok(modelService.getById(id));
    }

    @GetMapping("/list")
    public ApiResponse<PageResponse<Model>> list(@RequestParam(required = false) String tenantId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        Page<Model> p = modelService.list(tenantId, page, size);
        return ApiResponse.ok(PageResponse.of(p.getContent(), p.getTotalElements(), page, size));
    }

    @GetMapping("/{id}/attributes")
    public ApiResponse<List<ModelAttribute>> listAttributes(@PathVariable String id) {
        return ApiResponse.ok(modelService.listAttributes(id));
    }

    @GetMapping("/{id}/code-fields")
    public ApiResponse<List<ModelAttribute>> listCodeFields(@PathVariable String id) {
        return ApiResponse.ok(modelService.listCodeFields(id));
    }

    @PostMapping("/{id}/attributes")
    public ApiResponse<ModelAttribute> addAttribute(@PathVariable String id, @RequestBody ModelAttributeRequest req,
                                                    @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Attribute added", modelService.addAttribute(id, req, operatorId));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Model> publish(@PathVariable String id,
                                      @RequestParam(defaultValue = "true") boolean publishPhysicalTable,
                                      @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Model published", modelPublishService.publish(id, publishPhysicalTable, operatorId));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<Model> disable(@PathVariable String id,
                                      @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Model disabled", modelPublishService.disable(id, operatorId));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<Model> enable(@PathVariable String id,
                                     @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Model enabled", modelPublishService.enable(id, operatorId));
    }

    @GetMapping("/dbscript/dialect")
    public ApiResponse<Map<String, String>> getDialect() {
        return ApiResponse.ok(Map.of("dialect", modelPublishService.detectDialect()));
    }
}
