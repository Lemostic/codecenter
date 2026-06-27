package com.meritdata.mdm.codecenter.presentation.controller;

import com.meritdata.mdm.codecenter.application.dto.BatchCodeGenerateRequest;
import com.meritdata.mdm.codecenter.application.dto.CodeGenerateRequest;
import com.meritdata.mdm.codecenter.application.dto.CodeGenerateResponse;
import com.meritdata.mdm.codecenter.application.service.CodeGenerateService;
import com.meritdata.mdm.codecenter.application.service.LifecycleService;
import com.meritdata.mdm.codecenter.application.service.WaterMarkService;
import com.meritdata.mdm.codecenter.common.api.ApiResponse;
import com.meritdata.mdm.codecenter.domain.enums.WasteType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 编码生成 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mdm/encode/codes")
@RequiredArgsConstructor
public class CodeController {

    private final CodeGenerateService codeGenerateService;
    private final LifecycleService lifecycleService;
    private final WaterMarkService waterMarkService;

    @PostMapping("/generate")
    public ApiResponse<CodeGenerateResponse> generate(@RequestBody CodeGenerateRequest request) {
        log.info("POST /generate: modelId={}, fieldId={}, dataId={}",
                request.getModelId(), request.getFieldId(), request.getDataId());
        CodeGenerateResponse resp = codeGenerateService.generate(request);
        return Boolean.TRUE.equals(resp.getSuccess())
                ? ApiResponse.ok("Code generated", resp)
                : ApiResponse.fail(resp.getErrorMessage(), "CODECENTER-CODE-3001");
    }

    @PostMapping("/batch-generate")
    public ApiResponse<List<CodeGenerateResponse>> batchGenerate(@RequestBody BatchCodeGenerateRequest request) {
        log.info("POST /batch-generate: modelId={}, fieldId={}, count={}",
                request.getModelId(), request.getFieldId(), request.getCount());
        List<CodeGenerateResponse> list = codeGenerateService.batchGenerate(request);
        long success = list.stream().filter(CodeGenerateResponse::getSuccess).count();
        return ApiResponse.ok("Batch generated: " + success + "/" + list.size(), list);
    }

    @PostMapping("/{allocationId}/confirm")
    public ApiResponse<Void> confirm(@PathVariable String allocationId, @RequestParam String code) {
        log.info("Confirm code: code={}, allocationId={}", code, allocationId);
        lifecycleService.confirm(code, "system");
        return ApiResponse.ok("Code confirmed", null);
    }

    @PostMapping("/{allocationId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String allocationId,
                                    @RequestParam String code,
                                    @RequestParam(defaultValue = "CANCEL") String wasteType) {
        log.info("Cancel code: code={}, wasteType={}", code, wasteType);
        lifecycleService.cancel(code, WasteType.valueOf(wasteType.toUpperCase()), "system");
        return ApiResponse.ok("Code cancelled", null);
    }

    @PostMapping("/recycle-by-data/{dataId}")
    public ApiResponse<Void> recycleByData(@PathVariable String dataId) {
        log.info("Recycle by data: dataId={}", dataId);
        lifecycleService.recycleByDataId(dataId);
        return ApiResponse.ok("Recycled", null);
    }

    @GetMapping("/water-mark/{bizTag}")
    public ApiResponse<Map<String, Object>> getWaterMark(@PathVariable String bizTag) {
        return ApiResponse.ok(Map.of("bizTag", bizTag, "water", 0L));
    }

    @PostMapping("/timeout-scan")
    public ApiResponse<Map<String, Object>> timeoutScan() {
        int count = lifecycleService.scanAndRecycleTimeout();
        return ApiResponse.ok("Scanned", Map.of("recycled", count));
    }
}
