package com.meritdata.mdm.codecenter.presentation.controller;

import com.meritdata.mdm.codecenter.application.dto.CodeSegmentRequest;
import com.meritdata.mdm.codecenter.application.service.CodeSegmentService;
import com.meritdata.mdm.codecenter.common.api.ApiResponse;
import com.meritdata.mdm.codecenter.common.api.PageResponse;
import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.domain.valueobject.config.SegmentTypeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 码段 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mdm/encode/segments")
@RequiredArgsConstructor
public class CodeSegmentController {

    private final CodeSegmentService codeSegmentService;

    @PostMapping
    public ApiResponse<CodeSegment> create(@RequestBody CodeSegmentRequest req,
                                           @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Segment created", codeSegmentService.create(req, operatorId));
    }

    @PutMapping("/{id}")
    public ApiResponse<CodeSegment> update(@PathVariable String id, @RequestBody CodeSegmentRequest req,
                                           @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Segment updated", codeSegmentService.update(id, req, operatorId));
    }

    @GetMapping("/{id}")
    public ApiResponse<CodeSegment> get(@PathVariable String id) {
        return ApiResponse.ok(codeSegmentService.getById(id));
    }

    @GetMapping("/list")
    public ApiResponse<PageResponse<CodeSegment>> list(@RequestParam(required = false) SegmentType type,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Page<CodeSegment> p = codeSegmentService.list(type, page, size);
        return ApiResponse.ok(PageResponse.of(p.getContent(), p.getTotalElements(), page, size));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<Void> archive(@PathVariable String id,
                                     @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        codeSegmentService.archive(id, operatorId);
        return ApiResponse.ok("Segment archived", null);
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable String id,
                                     @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        codeSegmentService.restore(id, operatorId);
        return ApiResponse.ok("Segment restored", null);
    }

    @PostMapping("/preview")
    public ApiResponse<Map<String, Object>> previewConfig(@RequestBody Map<String, String> req) {
        SegmentType type = SegmentType.fromCode(req.get("type"));
        String configJson = req.get("configJson");
        SegmentTypeConfig cfg = codeSegmentService.previewConfig(type, configJson);
        return ApiResponse.ok(Map.of(
                "type", type.name(),
                "config", cfg.toString()));
    }
}
