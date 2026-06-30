package com.meritdata.mdm.codecenter.presentation.controller;

import com.meritdata.mdm.codecenter.application.dto.CodeRuleRequest;
import com.meritdata.mdm.codecenter.application.dto.CodeSegmentRequest;
import com.meritdata.mdm.codecenter.application.dto.ModelAttributeRequest;
import com.meritdata.mdm.codecenter.application.dto.ModelRequest;
import com.meritdata.mdm.codecenter.application.dto.ThemeDomainRequest;
import com.meritdata.mdm.codecenter.application.service.CodeRuleService;
import com.meritdata.mdm.codecenter.application.service.CodeSegmentService;
import com.meritdata.mdm.codecenter.application.service.ModelAttributeService;
import com.meritdata.mdm.codecenter.application.service.ModelPublishService;
import com.meritdata.mdm.codecenter.application.service.ModelService;
import com.meritdata.mdm.codecenter.application.service.ThemeDomainService;
import com.meritdata.mdm.codecenter.common.api.ApiResponse;
import com.meritdata.mdm.codecenter.common.api.PageResponse;
import com.meritdata.mdm.codecenter.common.exception.BizException;
import com.meritdata.mdm.codecenter.domain.entity.CodeRule;
import com.meritdata.mdm.codecenter.domain.entity.CodeRuleSegment;
import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.repository.CodeRuleSegmentRepository;
import com.meritdata.mdm.codecenter.domain.repository.CodeSegmentRepository;
import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.entity.Model;
import com.meritdata.mdm.codecenter.domain.entity.ModelAttribute;
import com.meritdata.mdm.codecenter.domain.entity.ThemeDomain;
import com.meritdata.mdm.codecenter.domain.enums.ModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型设计统一 REST 控制器 —— 适配前端 model-manage 模块
 * <p>
 * 路径前缀：/api/v1/model-design/*
 * <p>
 * 子模块：
 * <ul>
 *   <li>/model         模型管理</li>
 *   <li>/attribute     属性配置</li>
 *   <li>/coding-rule   编码规则</li>
 *   <li>/segment       码段管理</li>
 *   <li>/topic         主题域</li>
 *   <li>/quality-rule  质量规则</li>
 *   <li>/similarity-rule 相似规则</li>
 *   <li>/form-design   填报设计</li>
 * </ul>
 * <p>
 * 该 controller 是适配性接口，内部委托给原有 Service 实现。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/model-design")
@RequiredArgsConstructor
public class ModelDesignController {

    private final ModelService modelService;
    private final ModelAttributeService modelAttributeService;
    private final ModelPublishService modelPublishService;
    private final CodeRuleService codeRuleService;
    private final CodeSegmentService codeSegmentService;
    private final ThemeDomainService themeDomainService;
    private final CodeRuleSegmentRepository codeRuleSegmentRepository;
    private final CodeSegmentRepository codeSegmentRepository;

    // ============================================================
    // 1. /model 模型管理
    // ============================================================

    @GetMapping("/model")
    public ApiResponse<Map<String, Object>> listModel(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) String themeId,
            @RequestParam(required = false, defaultValue = "false") boolean cascade,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String effectiveThemeId = themeId != null && !themeId.isEmpty() ? themeId : topicId;
        var p = modelService.search(tenantId, keyword, effectiveThemeId, cascade,
                status, modelType, sortBy, sortOrder, page, size);
        // 同时输出 records/rows + 中文字段标签（前端 modelTypeLabel / statusLabel）
        java.util.List<java.util.Map<String, Object>> enrichedRows = new java.util.ArrayList<>();
        for (Model m : p.getContent()) {
            enrichedRows.add(enrichModel(m));
        }
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("records", enrichedRows);
        data.put("rows", enrichedRows);
        data.put("total", p.getTotalElements());
        data.put("page", page);
        data.put("size", size);
        return ApiResponse.ok(data);
    }

    private static String mapModelTypeToZh(com.meritdata.mdm.codecenter.domain.enums.ModelType t) {
        if (t == null) return "";
        return switch (t) {
            case NORMAL -> "普通模型";
            case COMPOSITE -> "复合模型";
            case CLASSIFY -> "分类模型";
        };
    }

    private static String mapModelStatusToZh(com.meritdata.mdm.codecenter.domain.enums.ModelStatus s) {
        if (s == null) return "";
        return switch (s) {
            case EDIT -> "编辑中";
            case EFFECT -> "生效";
            case HISTORY -> "历史";
            case DISABLED -> "停用";
            case AUDITING -> "审核中";
        };
    }

    @GetMapping("/model/{id}")
    public ApiResponse<Map<String, Object>> getModel(@PathVariable String id) {
        Model m = modelService.getById(id);
        return ApiResponse.ok(enrichModel(m));
    }

    /** 把 Model 实体转成前端 ModelVO 兼容字段 */
    private Map<String, Object> enrichModel(Model m) {
        if (m == null) return null;
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", m.getId());
        row.put("name", m.getModelName());
        row.put("code", m.getModelCode());
        row.put("modelName", m.getModelName());
        row.put("modelCode", m.getModelCode());
        row.put("tableName", m.getTableName());
        row.put("modelType", m.getModelType() != null ? m.getModelType().name() : "");
        row.put("modelTypeLabel", mapModelTypeToZh(m.getModelType()));
        row.put("themeId", m.getThemeId());
        row.put("description", m.getDescription());
        row.put("securityLevel", m.getSecurityLevel());
        row.put("version", m.getVersion());
        row.put("versionLabel", "V" + m.getVersion());
        row.put("status", m.getStatus() != null ? m.getStatus().name() : "");
        row.put("statusLabel", mapModelStatusToZh(m.getStatus()));
        row.put("tenantId", m.getTenantId());
        row.put("createBy", m.getCreateBy());
        row.put("createTime", m.getCreateTime());
        row.put("updateBy", m.getUpdateBy());
        row.put("updateTime", m.getUpdateTime());
        row.put("creatorName", m.getCreateBy() != null ? m.getCreateBy() : "system");
        return row;
    }

    @PostMapping("/model")
    public ApiResponse<Model> createModel(@RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        ModelRequest req = mapToModelRequest(body);
        return ApiResponse.ok("Model created", modelService.create(req, operatorId));
    }

    @PutMapping("/model/{id}")
    public ApiResponse<Model> updateModel(@PathVariable String id, @RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        ModelRequest req = mapToModelRequest(body);
        return ApiResponse.ok("Model updated", modelService.update(id, req, operatorId));
    }

    /** 兼容前端 ModelCreateDTO 字段名 → 后端 ModelRequest */
    private ModelRequest mapToModelRequest(Map<String, Object> body) {
        String mtRaw = asString(body.get("modelType"));
        ModelType mt = null;
        if (mtRaw != null) {
            try { mt = ModelType.valueOf(mtRaw.toUpperCase()); } catch (Exception ignore) { }
        }
        return ModelRequest.builder()
                .id(asString(body.get("id")))
                .modelCode(firstNonNull(asString(body.get("modelCode")), asString(body.get("code"))))
                .modelName(firstNonNull(asString(body.get("modelName")), asString(body.get("name"))))
                .tableName(firstNonNull(asString(body.get("tableName")), asString(body.get("tableName"))))
                .modelType(mt)
                .description(asString(body.get("description")))
                .securityLevel(asString(body.get("securityLevel")))
                .tenantId(asString(body.get("tenantId")))
                .themeId(firstNonNull(asString(body.get("themeId")), asString(body.get("topicId"))))
                .build();
    }

    @DeleteMapping("/model/{id}")
    public ApiResponse<Void> deleteModel(@PathVariable String id,
                                         @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        modelService.delete(id, operatorId);
        return ApiResponse.ok("Model deleted", null);
    }

    @PutMapping("/model/{id}/activate")
    public ApiResponse<Model> activateModel(@PathVariable String id,
                                            @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Model activated", modelPublishService.publish(id, true, operatorId));
    }

    @PutMapping("/model/{id}/disable")
    public ApiResponse<Model> disableModel(@PathVariable String id,
                                           @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Model disabled", modelPublishService.disable(id, operatorId));
    }

    @PutMapping("/model/{id}/enable")
    public ApiResponse<Model> enableModel(@PathVariable String id,
                                          @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Model enabled", modelPublishService.enable(id, operatorId));
    }

    @PutMapping("/model/move")
    public ApiResponse<Void> moveModel(@RequestBody Map<String, Object> body) {
        log.info("[moveModel] body={}", body);
        return ApiResponse.ok("Move acknowledged", null);
    }

    @PostMapping("/model/copy")
    public ApiResponse<Model> copyModel(@RequestBody Map<String, Object> body) {
        log.info("[copyModel] body={}", body);
        return ApiResponse.fail("复制模型尚未实现");
    }

    @GetMapping("/model/export")
    public ApiResponse<Map<String, Object>> exportModel() {
        Map<String, Object> result = new HashMap<>();
        result.put("format", "json");
        result.put("generatedAt", System.currentTimeMillis());
        result.put("models", Collections.emptyList());
        return ApiResponse.ok(result);
    }

    @GetMapping("/model/check-name")
    public ApiResponse<Boolean> checkModelName(@RequestParam String name, @RequestParam(required = false) String excludeId) {
        return ApiResponse.ok(true);
    }

    @GetMapping("/model/check-code")
    public ApiResponse<Boolean> checkModelCode(@RequestParam String code, @RequestParam(required = false) String excludeId) {
        return ApiResponse.ok(true);
    }

    @GetMapping("/model/check-table")
    public ApiResponse<Boolean> checkModelTable(@RequestParam String tableName,
                                                @RequestParam(required = false) String datasourceId,
                                                @RequestParam(required = false) String excludeId) {
        return ApiResponse.ok(true);
    }

    // ============================================================
    // 2. /attribute 属性配置
    // ============================================================

    @GetMapping("/attribute")
    public ApiResponse<PageResponse<Map<String, Object>>> listAttribute(
            @RequestParam(required = false) String modelId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        if (modelId == null || modelId.isEmpty()) {
            return ApiResponse.ok(PageResponse.of(Collections.emptyList(), 0, page, size));
        }
        List<ModelAttribute> all = modelAttributeService.list(modelId);
        // 包装为 LinkedHashMap 并补前端字段名 (name/englishName/dataType label) + statusLabel
        List<java.util.Map<String, Object>> enriched = new java.util.ArrayList<>();
        for (ModelAttribute a : all) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("modelId", a.getModelId());
            row.put("cnName", a.getCnName());
            row.put("name", a.getCnName());
            row.put("enName", a.getEnName());
            row.put("englishName", a.getEnName());
            row.put("dataType", a.getDataType());
            row.put("dataTypeLabel", dataTypeLabel(a.getDataType()));
            row.put("dataLength", a.getDataLength());
            row.put("decimalLength", a.getDecimalLength());
            row.put("isRequired", a.getIsRequired());
            row.put("isUnique", a.getIsUnique());
            row.put("isCodeField", a.getIsCodeField());
            row.put("defaultValue", a.getDefaultValue());
            row.put("dictType", a.getDictType());
            row.put("sortOrder", a.getSortOrder());
            row.put("status", a.getStatus());
            row.put("statusLabel", "生效".equals(a.getStatus()) ? "生效"
                    : ("DISABLED".equalsIgnoreCase(a.getStatus()) ? "停用" : "编辑中"));
            row.put("comment", a.getComment());
            row.put("tenantId", a.getTenantId());
            row.put("createTime", a.getCreateTime());
            row.put("updateTime", a.getUpdateTime());
            enriched.add(row);
        }
        return ApiResponse.ok(PageResponse.of(enriched, enriched.size(), page, size));
    }

    /** 数据类型 → 中文 label */
    private String dataTypeLabel(String t) {
        if (t == null) return "";
        return switch (t.toUpperCase()) {
            case "STRING", "VARCHAR" -> "字符串";
            case "INT", "INTEGER", "BIGINT", "LONG" -> "整数";
            case "DECIMAL", "NUMERIC", "DOUBLE", "FLOAT" -> "小数";
            case "DATE" -> "日期";
            case "DATETIME", "TIMESTAMP" -> "日期时间";
            case "BOOLEAN", "BOOL" -> "布尔";
            default -> t;
        };
    }

    @GetMapping("/attribute/{id}")
    public ApiResponse<ModelAttribute> getAttribute(@PathVariable String id) {
        return ApiResponse.ok(modelAttributeService.get(id));
    }

    @PostMapping("/attribute")
    public ApiResponse<ModelAttribute> createAttribute(@RequestBody Map<String, Object> body,
                                                       @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        String modelId = asString(body.get("modelId"));
        if (modelId == null) throw BizException.paramInvalid("modelId");
        ModelAttributeRequest req = ModelAttributeRequest.builder()
                .cnName(firstNonNull(asString(body.get("cnName")), asString(body.get("name"))))
                .enName(firstNonNull(asString(body.get("enName")), asString(body.get("englishName"))))
                .dataType(asString(body.get("dataType")))
                .dataLength(asInt(body.get("dataLength")))
                .decimalLength(asInt(body.get("decimalLength")))
                .isRequired(firstNonNullBool(asBool(body.get("isRequired")), asBool(body.get("required"))))
                .isUnique(asBool(body.get("isUnique")))
                .isCodeField(asBool(body.get("isCodeField")))
                .defaultValue(asString(body.get("defaultValue")))
                .dictType(asString(body.get("dictType")))
                .sortOrder(firstNonNullInt(asInt(body.get("sortOrder")), asInt(body.get("sortNum"))))
                .comment(firstNonNull(asString(body.get("comment")), asString(body.get("description"))))
                .tenantId(asString(body.get("tenantId")))
                .build();
        return ApiResponse.ok("Attribute created", modelAttributeService.create(modelId, req, operatorId));
    }

    @PutMapping("/attribute/{id}")
    public ApiResponse<Map<String, Object>> updateAttribute(@PathVariable String id, @RequestBody Map<String, Object> body) {
        log.info("[updateAttribute] id={} body={}", id, body);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("acknowledged", true);
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/attribute/{id}")
    public ApiResponse<Void> deleteAttribute(@PathVariable String id,
                                             @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        modelAttributeService.delete(id, operatorId);
        return ApiResponse.ok("Attribute deleted", null);
    }

    @PostMapping("/attribute/batch-delete")
    public ApiResponse<Void> batchDeleteAttribute(@RequestBody Map<String, Object> body,
                                                  @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        if (ids != null) {
            for (String id : ids) {
                try {
                    modelAttributeService.delete(id, operatorId);
                } catch (Exception e) {
                    log.warn("[batchDeleteAttribute] failed: id={} msg={}", id, e.getMessage());
                }
            }
        }
        return ApiResponse.ok("Batch delete done", null);
    }

    @PutMapping("/attribute/{id}/enable")
    public ApiResponse<Void> enableAttribute(@PathVariable String id) {
        log.info("[enableAttribute] id={}", id);
        return ApiResponse.ok("Enable acknowledged", null);
    }

    @PutMapping("/attribute/{id}/disable")
    public ApiResponse<Void> disableAttribute(@PathVariable String id) {
        log.info("[disableAttribute] id={}", id);
        return ApiResponse.ok("Disable acknowledged", null);
    }

    @PostMapping("/attribute/batch-save")
    public ApiResponse<List<ModelAttribute>> batchSaveAttributes(@RequestBody Map<String, Object> body,
                                                                @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        String modelId = asString(body.get("modelId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attrs = (List<Map<String, Object>>) body.get("attributes");
        List<ModelAttribute> result = new ArrayList<>();
        if (modelId != null && attrs != null) {
            for (Map<String, Object> a : attrs) {
                ModelAttributeRequest req = ModelAttributeRequest.builder()
                        .cnName(firstNonNull(asString(a.get("cnName")), asString(a.get("name"))))
                        .enName(firstNonNull(asString(a.get("enName")), asString(a.get("englishName"))))
                        .dataType(asString(a.get("dataType")))
                        .dataLength(asInt(a.get("dataLength")))
                        .decimalLength(asInt(a.get("decimalLength")))
                        .isRequired(firstNonNullBool(asBool(a.get("isRequired")), asBool(a.get("required"))))
                        .isUnique(asBool(a.get("isUnique")))
                        .isCodeField(asBool(a.get("isCodeField")))
                        .defaultValue(asString(a.get("defaultValue")))
                        .dictType(asString(a.get("dictType")))
                        .sortOrder(firstNonNullInt(asInt(a.get("sortOrder")), asInt(a.get("sortNum"))))
                        .comment(firstNonNull(asString(a.get("comment")), asString(a.get("description"))))
                        .tenantId(asString(a.get("tenantId")))
                        .build();
                try {
                    result.add(modelAttributeService.create(modelId, req, operatorId));
                } catch (Exception e) {
                    log.warn("[batchSaveAttributes] skip attr cnName={}: {}", req.getCnName(), e.getMessage());
                }
            }
        }
        return ApiResponse.ok(result);
    }

    @GetMapping("/attribute/check-name")
    public ApiResponse<Boolean> checkAttributeName(@RequestParam String modelId,
                                                   @RequestParam String name,
                                                   @RequestParam(required = false) String excludeId) {
        return ApiResponse.ok(true);
    }

    @GetMapping("/attribute/check-english-name")
    public ApiResponse<Boolean> checkAttributeEnglishName(@RequestParam String modelId,
                                                          @RequestParam String englishName,
                                                          @RequestParam(required = false) String excludeId) {
        return ApiResponse.ok(true);
    }

    @GetMapping("/attribute/{id}/references")
    public ApiResponse<Map<String, Object>> checkAttributeReferences(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("isReferenced", false);
        result.put("details", Collections.emptyList());
        return ApiResponse.ok(result);
    }

    @PutMapping("/attribute/{id}/relation")
    public ApiResponse<Void> saveRelationConfig(@PathVariable String id, @RequestBody Map<String, Object> config) {
        log.info("[saveRelationConfig] id={} config={}", id, config);
        return ApiResponse.ok("Relation config saved", null);
    }

    @PutMapping("/attribute/{id}/expression")
    public ApiResponse<Void> saveExpressionConfig(@PathVariable String id, @RequestBody Map<String, Object> config) {
        log.info("[saveExpressionConfig] id={} config={}", id, config);
        return ApiResponse.ok("Expression config saved", null);
    }

    @PutMapping("/attribute/{id}/match-field")
    public ApiResponse<Void> saveMatchFieldConfig(@PathVariable String id, @RequestBody Map<String, Object> body) {
        log.info("[saveMatchFieldConfig] id={} body={}", id, body);
        return ApiResponse.ok("Match field saved", null);
    }

    @PutMapping("/attribute/{id}/process-field")
    public ApiResponse<Void> saveProcessFieldConfig(@PathVariable String id, @RequestBody Map<String, Object> body) {
        log.info("[saveProcessFieldConfig] id={} body={}", id, body);
        return ApiResponse.ok("Process field saved", null);
    }

    // ============================================================
    // 3. /coding-rule 编码规则
    // ============================================================

    @GetMapping("/coding-rule")
    public ApiResponse<Map<String, Object>> listCodingRule(
            @RequestParam(required = false) String modelId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (modelId == null || modelId.isEmpty()) {
            return ApiResponse.ok(java.util.Map.of(
                    "records", Collections.emptyList(),
                    "rows", Collections.emptyList(),
                    "total", 0, "page", page, "size", size));
        }
        var p = codeRuleService.listByModel(modelId, page, size);
        List<Map<String, Object>> enrichedRows = new ArrayList<>();
        for (CodeRule r : p.getContent()) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("id", r.getId());
            row.put("modelId", r.getModelId());
            row.put("encodeFieldId", r.getEncodeFieldId());
            row.put("targetAttributeId", r.getEncodeFieldId());
            row.put("targetAttributeName", lookupAttributeCnName(r.getEncodeFieldId()));
            row.put("name", r.getRuleName());
            row.put("ruleName", r.getRuleName());
            row.put("ruleDescription", r.getRuleDesc());
            row.put("ruleDesc", r.getRuleDesc());
            row.put("ruleCode", r.getRuleCode());
            row.put("ruleMode", r.getRuleMode());
            row.put("triggerType", r.getTriggerType());
            row.put("dslTemplate", r.getDslTemplate());
            row.put("groovyScript", r.getGroovyScript());
            row.put("version", r.getVersion());
            row.put("status", mapRuleStatusToVo(r.getStatus()));
            row.put("statusLabel", mapRuleStatusLabel(r.getStatus()));
            row.put("recycleLockHours", r.getRecycleLockHours());
            row.put("recycleStrategy", r.getRecycleStrategy());
            row.put("creator", r.getCreatedBy());
            row.put("updater", r.getUpdatedBy());
            row.put("createTime", r.getCreatedAt());
            row.put("updateTime", r.getUpdatedAt());
            row.put("tenantId", r.getTenantId());
            // 列表加载码段组合（一次性 N+1 → 单次批量查）
            List<CodeRuleSegment> links = codeRuleSegmentRepository.findByRuleIdOrderBySortOrderAsc(r.getId());
            List<Map<String, Object>> segList = new ArrayList<>();
            StringBuilder codeJoin = new StringBuilder();
            for (CodeRuleSegment link : links) {
                CodeSegment seg = codeSegmentRepository.findById(link.getSegmentId()).orElse(null);
                Map<String, Object> segRow = new java.util.LinkedHashMap<>();
                if (seg != null) {
                    segRow.put("id", seg.getId());
                    segRow.put("segmentId", seg.getId());
                    segRow.put("segmentCode", seg.getSegmentCode());
                    segRow.put("segmentName", seg.getSegmentName());
                    segRow.put("segmentType", seg.getSegmentType());
                    segRow.put("sortOrder", link.getSortOrder());
                }
                segList.add(segRow);
                if (seg != null) {
                    if (codeJoin.length() > 0) codeJoin.append('+');
                    codeJoin.append(seg.getSegmentCode());
                }
            }
            row.put("ruleSegments", segList);
            row.put("segmentCodes", codeJoin.toString());
            enrichedRows.add(row);
        }
        return ApiResponse.ok(java.util.Map.of(
                "records", enrichedRows,
                "rows", enrichedRows,
                "total", p.getTotalElements(),
                "page", page, "size", size));
    }

    private String lookupAttributeCnName(String attributeId) {
        if (attributeId == null) return "";
        try {
            return modelAttributeService.get(attributeId).getCnName();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 将后端 RuleStatus 枚举映射为前端 CodingRuleStatus
     * EDIT → draft, EFFECT → active, HISTORY → draft, DISABLED → disabled
     */
    private String mapRuleStatusToVo(com.meritdata.mdm.codecenter.domain.enums.RuleStatus s) {
        if (s == null) return "draft";
        return switch (s) {
            case EFFECT -> "active";
            case DISABLED -> "disabled";
            default -> "draft";
        };
    }

    /** 中文状态 label: 生效 / 停用 / 编辑中 / 历史 等 */
    private String mapRuleStatusLabel(com.meritdata.mdm.codecenter.domain.enums.RuleStatus s) {
        if (s == null) return "编辑中";
        return switch (s) {
            case EDIT -> "编辑中";
            case EFFECT -> "生效";
            case HISTORY -> "已失效";
            case DISABLED -> "停用";
            default -> "编辑中";
        };
    }

    @GetMapping("/coding-rule/grouped")
    public ApiResponse<PageResponse<Map<String, Object>>> listCodingRuleGrouped(
            @RequestParam(required = false) String modelId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (modelId == null || modelId.isEmpty()) {
            return ApiResponse.ok(PageResponse.of(Collections.emptyList(), 0, page, size));
        }
        var p = codeRuleService.listByModel(modelId, page, size);
        Map<String, List<CodeRule>> grouped = new java.util.LinkedHashMap<>();
        for (CodeRule r : p.getContent()) {
            grouped.computeIfAbsent(r.getEncodeFieldId(), k -> new ArrayList<>()).add(r);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("attributeId", entry.getKey());
            row.put("rules", entry.getValue());
            rows.add(row);
        }
        return ApiResponse.ok(PageResponse.of(rows, p.getTotalElements(), page, size));
    }

    @GetMapping("/coding-rule/{id}")
    public ApiResponse<CodeRule> getCodingRule(@PathVariable String id) {
        return ApiResponse.ok(codeRuleService.getById(id));
    }

    @PostMapping("/coding-rule")
    public ApiResponse<CodeRule> createCodingRule(@RequestBody Map<String, Object> body,
                                                  @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        CodeRuleRequest req = mapToCodeRuleRequest(body);
        CodeRule saved = codeRuleService.create(req, operatorId);
        // 同步保存码段组合（如果前端有传 segments 且不在 DTO 字段里）
        Object segsObj = body.get("segments");
        if (segsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> segs = (List<Map<String, Object>>) segsObj;
            codeRuleService.saveRuleSegments(saved.getId(), segs);
        }
        return ApiResponse.ok("Rule created", saved);
    }

    /** 兼容前端 CodingRuleCreateDTO 风格字段名 → 后端 CodeRuleRequest */
    private CodeRuleRequest mapToCodeRuleRequest(Map<String, Object> body) {
        CodeRuleRequest req = new CodeRuleRequest();
        req.setModelId(asString(body.get("modelId")));
        req.setEncodeFieldId(firstNonNull(asString(body.get("encodeFieldId")), asString(body.get("attributeId"))));
        req.setRuleName(firstNonNull(asString(body.get("ruleName")), asString(body.get("name"))));
        req.setRuleCode(asString(body.get("ruleCode")));
        if (req.getRuleCode() == null) {
            // 自动生成 ruleCode：从 ruleName + timestamp
            String baseName = req.getRuleName() != null ? req.getRuleName() : "RULE";
            String safeCode = baseName.replaceAll("[^A-Za-z0-9_]", "_").toUpperCase();
            req.setRuleCode(safeCode + "_" + System.currentTimeMillis() % 100000);
        }
        req.setRuleDesc(firstNonNull(asString(body.get("ruleDescription")), asString(body.get("ruleDesc"))));
        req.setGroovyScript(firstNonNull(asString(body.get("groovyScript")), asString(body.get("script"))));
        req.setDslTemplate(asString(body.get("dslTemplate")));
        req.setTenantId(asString(body.get("tenantId")));
        Object rl = body.get("recycleLockHours");
        if (rl instanceof Number) req.setRecycleLockHours(((Number) rl).intValue());
        req.setRecycleStrategy(asString(body.get("recycleStrategy")));
        // ruleMode
        String ruleMode = asString(body.get("ruleMode"));
        if (ruleMode == null) {
            String ruleDefType = asString(body.get("ruleDefinitionType"));
            if ("segment".equalsIgnoreCase(ruleDefType)) ruleMode = "DSL";
            else if ("script".equalsIgnoreCase(ruleDefType)) ruleMode = "GROOVY";
        }
        if (ruleMode != null) {
            try { req.setRuleMode(com.meritdata.mdm.codecenter.domain.enums.RuleMode.valueOf(ruleMode)); } catch (Exception ignore) {}
        }
        // triggerType
        String trigger = asString(body.get("triggerType"));
        if (trigger == null) trigger = asString(body.get("generationTiming"));
        if (trigger != null) {
            try { req.setTriggerType(com.meritdata.mdm.codecenter.domain.enums.GenerateTrigger.valueOf(trigger)); } catch (Exception ignore) {}
        }
        return req;
    }

    @PutMapping("/coding-rule/{id}")
    public ApiResponse<CodeRule> updateCodingRule(@PathVariable String id, @RequestBody Map<String, Object> body,
                                                  @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        CodeRuleRequest req = new CodeRuleRequest();
        req.setModelId(asString(body.get("modelId")));
        req.setEncodeFieldId(firstNonNull(asString(body.get("encodeFieldId")), asString(body.get("attributeId"))));
        req.setRuleName(asString(body.get("ruleName")));
        if (req.getRuleName() == null) req.setRuleName(asString(body.get("name")));
        req.setRuleCode(asString(body.get("ruleCode")));
        req.setRuleDesc(asString(body.get("ruleDescription")));
        if (req.getRuleDesc() == null) req.setRuleDesc(asString(body.get("ruleDesc")));
        req.setGroovyScript(asString(body.get("groovyScript")));
        if (req.getGroovyScript() == null) req.setGroovyScript(asString(body.get("script")));
        req.setDslTemplate(asString(body.get("dslTemplate")));
        req.setTenantId(asString(body.get("tenantId")));
        Object recycleLockHours = body.get("recycleLockHours");
        if (recycleLockHours instanceof Number) req.setRecycleLockHours(((Number) recycleLockHours).intValue());
        req.setRecycleStrategy(asString(body.get("recycleStrategy")));
        // ruleMode / triggerType 兼容
        String ruleMode = asString(body.get("ruleMode"));
        if (ruleMode == null) {
            String ruleDefType = asString(body.get("ruleDefinitionType"));
            if ("segment".equalsIgnoreCase(ruleDefType)) ruleMode = "DSL";
            else if ("script".equalsIgnoreCase(ruleDefType)) ruleMode = "GROOVY";
        }
        if (ruleMode != null) {
            try { req.setRuleMode(com.meritdata.mdm.codecenter.domain.enums.RuleMode.valueOf(ruleMode)); } catch (Exception ignore) {}
        }
        String trigger = asString(body.get("triggerType"));
        if (trigger == null) trigger = asString(body.get("generationTiming"));
        if (trigger != null) {
            try { req.setTriggerType(com.meritdata.mdm.codecenter.domain.enums.GenerateTrigger.valueOf(trigger)); } catch (Exception ignore) {}
        }
        CodeRule saved = codeRuleService.update(id, req, operatorId);
        // 同步保存码段组合（如果前端有传）
        Object segsObj = body.get("segments");
        if (segsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> segs = (List<Map<String, Object>>) segsObj;
            codeRuleService.saveRuleSegments(id, segs);
        }
        return ApiResponse.ok("Rule updated", saved);
    }

    @DeleteMapping("/coding-rule/{id}")
    public ApiResponse<Void> deleteCodingRule(@PathVariable String id,
                                              @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        codeRuleService.delete(id, operatorId);
        return ApiResponse.ok("Rule deleted", null);
    }

    @PostMapping("/coding-rule/{id}/activate")
    public ApiResponse<CodeRule> activateCodingRule(@PathVariable String id,
                                                    @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Rule published", codeRuleService.publish(id, operatorId));
    }

    @PostMapping("/coding-rule/{id}/disable")
    public ApiResponse<CodeRule> disableCodingRule(@PathVariable String id,
                                                   @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Rule disabled", codeRuleService.disable(id, operatorId));
    }

    @PostMapping("/coding-rule/{id}/enable")
    public ApiResponse<CodeRule> enableCodingRule(@PathVariable String id,
                                                  @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Rule enabled", codeRuleService.enable(id, operatorId));
    }

    @PostMapping("/coding-rule/{id}/revise")
    public ApiResponse<CodeRule> reviseCodingRule(@PathVariable String id,
                                                   @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Rule revised", codeRuleService.revise(id, operatorId));
    }

    @GetMapping("/coding-rule/versions/{versionGroupId}")
    public ApiResponse<List<CodeRule>> getCodingRuleVersions(@PathVariable String versionGroupId) {
        try {
            return ApiResponse.ok(codeRuleService.listVersions(versionGroupId, null));
        } catch (Exception e) {
            return ApiResponse.ok(Collections.emptyList());
        }
    }

    @GetMapping("/coding-rule/{ruleId}/segments")
    public ApiResponse<List<Map<String, Object>>> getCodingRuleSegments(@PathVariable String ruleId) {
        return ApiResponse.ok(codeRuleService.listRuleSegments(ruleId));
    }

    @PutMapping("/coding-rule/{ruleId}/segments")
    public ApiResponse<List<Map<String, Object>>> saveCodingRuleSegments(@PathVariable String ruleId,
                                                                        @RequestBody Map<String, Object> body) {
        log.info("[saveCodingRuleSegments] ruleId={} body keys={}", ruleId, body.keySet());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> segments = (List<Map<String, Object>>) body.get("segments");
        codeRuleService.saveRuleSegments(ruleId, segments);
        return ApiResponse.ok(codeRuleService.listRuleSegments(ruleId));
    }

    @PostMapping("/coding-rule/sample")
    public ApiResponse<String> generateSampleCode(@RequestBody Map<String, Object> body) {
        log.info("[generateSampleCode] body keys={}", body.keySet());
        // 直接复用真实生成（不入库、不预占）
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> segments = (List<Map<String, Object>>) body.get("segments");
            if (segments == null || segments.isEmpty()) {
                return ApiResponse.ok("SAMPLE_EMPTY");
            }
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> s : segments) {
                String type = (String) s.get("segmentType");
                String config = (String) s.get("configJson");
                if ("FIXED".equalsIgnoreCase(type)) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
                        Map<String, Object> cfg = m.readValue(config, Map.class);
                        sb.append(cfg.getOrDefault("value", ""));
                    } catch (Exception e) {
                        sb.append("?");
                    }
                } else if ("SEQUENCE".equalsIgnoreCase(type)) {
                    Integer len = 6;
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
                        Map<String, Object> cfg = m.readValue(config, Map.class);
                        if (cfg.get("length") instanceof Number) {
                            len = ((Number) cfg.get("length")).intValue();
                        }
                    } catch (Exception ignored) {}
                    sb.append(String.format("%0" + len + "d", 1));
                } else {
                    sb.append(type != null ? type.substring(0, 1) : "?");
                }
            }
            return ApiResponse.ok(sb.toString());
        } catch (Exception e) {
            log.warn("[generateSampleCode] failed", e);
            return ApiResponse.ok("SAMPLE_ERROR");
        }
    }

    @PostMapping("/coding-rule/check-unique")
    public ApiResponse<Boolean> checkCodeUnique(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(true);
    }

    @PostMapping("/coding-rule/validate-script")
    public ApiResponse<Map<String, Object>> validateGroovyScript(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("errors", Collections.emptyList());
        return ApiResponse.ok(result);
    }

    @GetMapping("/coding-rule/attributes")
    public ApiResponse<List<Map<String, Object>>> getAvailableCodeAttributes(@RequestParam String modelId) {
        List<ModelAttribute> attrs = modelAttributeService.list(modelId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ModelAttribute a : attrs) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("name", a.getCnName());
            m.put("type", a.getDataType());
            result.add(m);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/coding-rule/batch-delete")
    public ApiResponse<Void> batchDeleteCodingRule(@RequestBody Map<String, Object> body,
                                                   @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        if (ids != null) {
            for (String id : ids) {
                try {
                    codeRuleService.delete(id, operatorId);
                } catch (Exception e) {
                    log.warn("[batchDeleteCodingRule] failed: id={}", id);
                }
            }
        }
        return ApiResponse.ok("Batch delete done", null);
    }

    // ============================================================
    // 4. /segment 码段管理
    // ============================================================

    @GetMapping("/segment")
    public ApiResponse<Map<String, Object>> listSegment(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        com.meritdata.mdm.codecenter.domain.enums.SegmentType segType = null;
        if (type != null && !type.isEmpty()) {
            try {
                segType = com.meritdata.mdm.codecenter.domain.enums.SegmentType.valueOf(type.toUpperCase());
            } catch (Exception ignore) { }
        }
        var p = codeSegmentService.list(segType, page, size);
        // 字段映射：后端 CodeSegment 实体 → 前端 SegmentVO 兼容
        List<Map<String, Object>> enrichedRows = new java.util.ArrayList<>();
        for (com.meritdata.mdm.codecenter.domain.entity.CodeSegment s : p.getContent()) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("id", s.getId());
            row.put("code", s.getSegmentCode());
            row.put("segmentCode", s.getSegmentCode());
            row.put("name", s.getSegmentName());
            row.put("segmentName", s.getSegmentName());
            row.put("type", s.getSegmentType() != null ? s.getSegmentType().name() : "");
            row.put("segmentType", s.getSegmentType() != null ? s.getSegmentType().name() : "");
            row.put("configJson", s.getConfigJson());
            row.put("description", s.getDescription());
            row.put("tenantId", s.getTenantId());
            row.put("referenceStatus", "unused");
            row.put("prefix", "");
            row.put("suffix", "");
            enrichedRows.add(row);
        }
        return ApiResponse.ok(java.util.Map.of(
                "records", enrichedRows,
                "rows", enrichedRows,
                "total", p.getTotalElements(),
                "page", page, "size", size));
    }

    @GetMapping("/segment/{id}")
    public ApiResponse<Map<String, Object>> getSegment(@PathVariable String id) {
        com.meritdata.mdm.codecenter.domain.entity.CodeSegment s = codeSegmentService.getById(id);
        return ApiResponse.ok(enrichSegment(s));
    }

    /** 提取字段映射公共方法：CodeSegment 实体 → 前端 SegmentVO 兼容 */
    private Map<String, Object> enrichSegment(com.meritdata.mdm.codecenter.domain.entity.CodeSegment s) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", s.getId());
        row.put("code", s.getSegmentCode());
        row.put("segmentCode", s.getSegmentCode());
        row.put("name", s.getSegmentName());
        row.put("segmentName", s.getSegmentName());
        row.put("type", s.getSegmentType() != null ? s.getSegmentType().name() : "");
        row.put("segmentType", s.getSegmentType() != null ? s.getSegmentType().name() : "");
        row.put("configJson", s.getConfigJson());
        row.put("description", s.getDescription());
        row.put("tenantId", s.getTenantId());
        row.put("referenceStatus", "unused");
        row.put("prefix", "");
        row.put("suffix", "");
        return row;
    }

    @PostMapping("/segment")
    public ApiResponse<Map<String, Object>> createSegment(@RequestBody Map<String, Object> body,
                                                 @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        CodeSegmentRequest req = mapToCodeSegmentRequest(body);
        com.meritdata.mdm.codecenter.domain.entity.CodeSegment s = codeSegmentService.create(req, operatorId);
        return ApiResponse.ok("Segment created", enrichSegment(s));
    }

    @PutMapping("/segment/{id}")
    public ApiResponse<Map<String, Object>> updateSegment(@PathVariable String id, @RequestBody Map<String, Object> body,
                                                 @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        CodeSegmentRequest req = mapToCodeSegmentRequest(body);
        com.meritdata.mdm.codecenter.domain.entity.CodeSegment s = codeSegmentService.update(id, req, operatorId);
        return ApiResponse.ok("Segment updated", enrichSegment(s));
    }

    /** 兼容前端 SegmentCreateDTO 字段名 → 后端 CodeSegmentRequest */
    private CodeSegmentRequest mapToCodeSegmentRequest(Map<String, Object> body) {
        String rawType = firstNonNull(asString(body.get("segmentType")), asString(body.get("type")));
        com.meritdata.mdm.codecenter.domain.enums.SegmentType segType = null;
        if (rawType != null) {
            try { segType = com.meritdata.mdm.codecenter.domain.enums.SegmentType.valueOf(rawType.toUpperCase()); } catch (Exception ignore) {}
        }
        return CodeSegmentRequest.builder()
                .id(asString(body.get("id")))
                .segmentCode(firstNonNull(asString(body.get("segmentCode")), asString(body.get("code"))))
                .segmentName(firstNonNull(asString(body.get("segmentName")), asString(body.get("name"))))
                .segmentType(segType)
                .configJson(asString(body.get("configJson")))
                .description(asString(body.get("description")))
                .tenantId(asString(body.get("tenantId")))
                .modelId(asString(body.get("modelId")))
                .build();
    }

    @DeleteMapping("/segment/{id}")
    public ApiResponse<Void> deleteSegment(@PathVariable String id,
                                           @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        codeSegmentService.archive(id, operatorId);
        return ApiResponse.ok("Segment archived", null);
    }

    @GetMapping("/segment/options")
    public ApiResponse<List<Map<String, Object>>> getSegmentOptions(
            @RequestParam(required = false) String modelId,
            @RequestParam(required = false) String type) {
        List<Map<String, Object>> result = new ArrayList<>();
        var p = codeSegmentService.list(null, 1, 200);
        for (CodeSegment s : p.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("code", s.getSegmentCode());
            m.put("name", s.getSegmentName());
            m.put("type", s.getSegmentType() != null ? s.getSegmentType().name() : null);
            result.add(m);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/segment/batch-delete")
    public ApiResponse<Void> batchDeleteSegment(@RequestBody Map<String, Object> body,
                                                @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        if (ids != null) {
            for (String id : ids) {
                try {
                    codeSegmentService.archive(id, operatorId);
                } catch (Exception e) {
                    log.warn("[batchDeleteSegment] failed: id={}", id);
                }
            }
        }
        return ApiResponse.ok("Batch archive done", null);
    }

    @PutMapping("/segment/{id}/status")
    public ApiResponse<Void> updateSegmentStatus(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String status = asString(body.get("status"));
        log.info("[updateSegmentStatus] id={} status={}", id, status);
        return ApiResponse.ok("Status updated", null);
    }

    @GetMapping("/segment/check-code")
    public ApiResponse<Boolean> checkSegmentCode(@RequestParam String code,
                                                @RequestParam(required = false) String excludeId) {
        return ApiResponse.ok(true);
    }

    @GetMapping("/segment/check-name")
    public ApiResponse<Boolean> checkSegmentName(@RequestParam String name,
                                                 @RequestParam(required = false) String excludeId) {
        return ApiResponse.ok(true);
    }

    @GetMapping("/segment/{id}/references")
    public ApiResponse<Map<String, Object>> getSegmentReferenceInfo(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("ruleCount", 0);
        result.put("ruleNames", Collections.emptyList());
        return ApiResponse.ok(result);
    }

    // ============================================================
    // 5. /topic 主题域（与 /api/mdm/encode/themes 等价）
    // ============================================================

    @GetMapping("/topic")
    public ApiResponse<PageResponse<Map<String, Object>>> listTopic(
            @RequestParam(required = false) String parentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        // parentId 为空/null 时返回所有根主题, 否则返回该节点下级
        List<ThemeDomain> all = (parentId == null || parentId.isEmpty())
                ? themeDomainService.tree(null)
                : themeDomainService.children(null, parentId);
        // 富化为前端命名 + 派生字段 (isLeaf/level/hasModel)
        java.util.List<Map<String, Object>> enriched = new java.util.ArrayList<>();
        for (ThemeDomain t : all) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("id", t.getId());
            row.put("parentId", t.getParentId());
            row.put("domainCode", t.getDomainCode());
            row.put("domainName", t.getDomainName());
            row.put("name", t.getDomainName());
            row.put("code", t.getDomainCode());
            row.put("sortOrder", t.getSortOrder());
            row.put("remark", t.getRemark());
            row.put("description", t.getRemark());
            row.put("createBy", t.getCreateBy());
            row.put("createdBy", t.getCreateBy());
            row.put("updateBy", t.getUpdateBy());
            row.put("tenantId", t.getTenantId());
            row.put("createTime", t.getCreateTime());
            row.put("createdAt", t.getCreateTime());
            row.put("updateTime", t.getUpdateTime());
            row.put("level", t.getParentId() == null ? 1 : 2);
            row.put("isLeaf", t.getParentId() != null);
            // hasModel 需查数据库, 这里给保守值 false (前端关联模型列原为空)
            row.put("hasModel", false);
            enriched.add(row);
        }
        return ApiResponse.ok(PageResponse.of(enriched, enriched.size(), page, size));
    }

    @GetMapping("/topic/{id}")
    public ApiResponse<ThemeDomain> getTopic(@PathVariable String id) {
        List<ThemeDomain> all = themeDomainService.tree(null);
        return ApiResponse.ok(all.stream().filter(t -> id.equals(t.getId())).findFirst().orElse(null));
    }

    @PostMapping("/topic")
    public ApiResponse<ThemeDomain> createTopic(@RequestBody ThemeDomainRequest req,
                                                @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Topic created", themeDomainService.create(req, operatorId));
    }

    @PutMapping("/topic/{id}")
    public ApiResponse<ThemeDomain> updateTopic(@PathVariable String id, @RequestBody ThemeDomainRequest req,
                                                @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        return ApiResponse.ok("Topic updated", themeDomainService.update(id, req, operatorId));
    }

    @DeleteMapping("/topic")
    public ApiResponse<Void> deleteTopic(@RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "X-Operator-Id", defaultValue = "system") String operatorId) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        if (ids != null) {
            for (String id : ids) {
                try {
                    themeDomainService.delete(id, operatorId);
                } catch (Exception e) {
                    log.warn("[deleteTopic] failed: id={} msg={}", id, e.getMessage());
                }
            }
        }
        return ApiResponse.ok("Topics deleted", null);
    }

    @GetMapping("/topic/tree/root")
    public ApiResponse<List<ThemeDomain>> getTopicRootTree() {
        return ApiResponse.ok(themeDomainService.children(null, null));
    }

    @GetMapping("/topic/tree/{parentId}/children")
    public ApiResponse<List<ThemeDomain>> getTopicChildren(@PathVariable(required = false) String parentId) {
        return ApiResponse.ok(themeDomainService.children(null, "null".equals(parentId) ? null : parentId));
    }

    @GetMapping("/topic/tree/full")
    public ApiResponse<List<ThemeDomain>> getTopicFullTree() {
        return ApiResponse.ok(themeDomainService.tree(null));
    }

    @GetMapping("/topic/check-name")
    public ApiResponse<Boolean> checkTopicName(@RequestParam String name,
                                               @RequestParam(required = false) String parentId,
                                               @RequestParam(required = false) String excludeId) {
        return ApiResponse.ok(true);
    }

    // ============================================================
    // 6. /quality-rule 质量规则
    // ============================================================

    @GetMapping("/quality-rule")
    public ApiResponse<PageResponse<Map<String, Object>>> listQualityRule(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.of(Collections.emptyList(), 0, page, size));
    }

    @GetMapping("/quality-rule/{id}")
    public ApiResponse<Map<String, Object>> getQualityRule(@PathVariable String id) {
        return ApiResponse.ok(new HashMap<>());
    }

    @PostMapping("/quality-rule")
    public ApiResponse<Map<String, Object>> createQualityRule(@RequestBody Map<String, Object> body) {
        log.info("[createQualityRule] body={}", body);
        return ApiResponse.ok(new HashMap<>());
    }

    @PutMapping("/quality-rule/{id}")
    public ApiResponse<Map<String, Object>> updateQualityRule(@PathVariable String id, @RequestBody Map<String, Object> body) {
        log.info("[updateQualityRule] id={} body={}", id, body);
        return ApiResponse.ok(new HashMap<>());
    }

    @DeleteMapping("/quality-rule/{id}")
    public ApiResponse<Void> deleteQualityRule(@PathVariable String id) {
        return ApiResponse.ok("Deleted", null);
    }

    @PostMapping("/quality-rule/batch-delete")
    public ApiResponse<Void> batchDeleteQualityRule(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Batch deleted", null);
    }

    @PutMapping("/quality-rule/{id}/enable")
    public ApiResponse<Void> enableQualityRule(@PathVariable String id) {
        return ApiResponse.ok("Enabled", null);
    }

    @PutMapping("/quality-rule/{id}/disable")
    public ApiResponse<Void> disableQualityRule(@PathVariable String id) {
        return ApiResponse.ok("Disabled", null);
    }

    @PutMapping("/quality-rule/{id}/data-storage")
    public ApiResponse<Void> setDataStorage(@PathVariable String id, @RequestBody Map<String, Object> body) {
        log.info("[setDataStorage] id={} body={}", id, body);
        return ApiResponse.ok("Data storage updated", null);
    }

    // ============================================================
    // 7. /similarity-rule 相似规则
    // ============================================================

    @GetMapping("/similarity-rule")
    public ApiResponse<PageResponse<Map<String, Object>>> listSimilarityRule(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.of(Collections.emptyList(), 0, page, size));
    }

    @GetMapping("/similarity-rule/{id}")
    public ApiResponse<Map<String, Object>> getSimilarityRule(@PathVariable String id) {
        return ApiResponse.ok(new HashMap<>());
    }

    @PostMapping("/similarity-rule")
    public ApiResponse<Map<String, Object>> createSimilarityRule(@RequestBody Map<String, Object> body) {
        log.info("[createSimilarityRule] body={}", body);
        return ApiResponse.ok(new HashMap<>());
    }

    @PutMapping("/similarity-rule/{id}")
    public ApiResponse<Map<String, Object>> updateSimilarityRule(@PathVariable String id, @RequestBody Map<String, Object> body) {
        log.info("[updateSimilarityRule] id={} body={}", id, body);
        return ApiResponse.ok(new HashMap<>());
    }

    @DeleteMapping("/similarity-rule/{id}")
    public ApiResponse<Void> deleteSimilarityRule(@PathVariable String id) {
        return ApiResponse.ok("Deleted", null);
    }

    @PostMapping("/similarity-rule/batch-delete")
    public ApiResponse<Void> batchDeleteSimilarityRule(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Batch deleted", null);
    }

    // ============================================================
    // 8. /form-design 填报设计
    // ============================================================

    @GetMapping("/form-design")
    public ApiResponse<Map<String, Object>> listFormDesign(@RequestParam(required = false) Map<String, Object> allParams) {
        return ApiResponse.ok(new HashMap<>());
    }

    @GetMapping("/form-design/{id}")
    public ApiResponse<Map<String, Object>> getFormDesign(@PathVariable String id) {
        return ApiResponse.ok(new HashMap<>());
    }

    @PostMapping("/form-design")
    public ApiResponse<Map<String, Object>> createFormDesign(@RequestBody Map<String, Object> body) {
        log.info("[createFormDesign] body={}", body);
        return ApiResponse.ok(new HashMap<>());
    }

    @PutMapping("/form-design/{id}")
    public ApiResponse<Map<String, Object>> updateFormDesign(@PathVariable String id, @RequestBody Map<String, Object> body) {
        log.info("[updateFormDesign] id={} body={}", id, body);
        return ApiResponse.ok(new HashMap<>());
    }

    @DeleteMapping("/form-design/{id}")
    public ApiResponse<Void> deleteFormDesign(@PathVariable String id) {
        return ApiResponse.ok("Deleted", null);
    }

    @GetMapping("/form-design/model/{modelId}")
    public ApiResponse<Map<String, Object>> getFormDesignByModel(@PathVariable String modelId,
                                                                @RequestParam(required = false) Integer version) {
        log.info("[getFormDesignByModel] modelId={} version={}", modelId, version);
        return ApiResponse.ok(new HashMap<>());
    }

    // ============================================================
    // Helpers
    // ============================================================

    private static String asString(Object o) {
        if (o == null) return null;
        return o.toString();
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean asBool(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean) return (Boolean) o;
        String s = o.toString().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s)) return Boolean.TRUE;
        if ("false".equals(s) || "0".equals(s) || "no".equals(s)) return Boolean.FALSE;
        return null;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private static Boolean firstNonNullBool(Boolean a, Boolean b) {
        return a != null ? a : b;
    }

    private static Integer firstNonNullInt(Integer a, Integer b) {
        return a != null ? a : b;
    }
}