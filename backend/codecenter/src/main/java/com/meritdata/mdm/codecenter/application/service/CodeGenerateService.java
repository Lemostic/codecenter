package com.meritdata.mdm.codecenter.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meritdata.mdm.codecenter.application.cache.RuleBundle;
import com.meritdata.mdm.codecenter.application.cache.RuleCacheService;
import com.meritdata.mdm.codecenter.application.dto.BatchCodeGenerateRequest;
import com.meritdata.mdm.codecenter.application.dto.CodeGenerateRequest;
import com.meritdata.mdm.codecenter.application.dto.CodeGenerateResponse;
import com.meritdata.mdm.codecenter.common.exception.BizException;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.*;
import com.meritdata.mdm.codecenter.domain.enums.AllocationStatus;
import com.meritdata.mdm.codecenter.domain.enums.RuleMode;
import com.meritdata.mdm.codecenter.domain.enums.RuleStatus;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.domain.repository.*;
import com.meritdata.mdm.codecenter.domain.valueobject.FormatTemplate;
import com.meritdata.mdm.codecenter.infrastructure.dedup.CodeDedupService;
import com.meritdata.mdm.codecenter.infrastructure.dsl.DslEngine;
import com.meritdata.mdm.codecenter.infrastructure.dsl.GroovyScriptEngine;
import com.meritdata.mdm.codecenter.infrastructure.segment.SegmentContext;
import com.meritdata.mdm.codecenter.infrastructure.segment.SegmentResult;
import com.meritdata.mdm.codecenter.infrastructure.sequence.SequenceGenerator;
import com.meritdata.mdm.codecenter.infrastructure.dbscript.PhysicalTablePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 编码生成核心服务
 *
 * 单条生成流程 (V0.9 优化后):
 *   1. RuleCacheService 取规则+码段快照 (1 次 DB 或 0 次, 命中率高)
 *   2. DSL 模式 -> 解析模板 -> 串行执行码段
 *      GROOVY 模式 -> 执行脚本
 *   3. 三级防重 (L1 Caffeine -> L2 Redis -> L3 DB)
 *   4. 持久化到 md_code_allocation
 *   5. 更新水位
 *
 * 批量生成:
 *   V0.3 单源原则:
 *     1. 决策回收池优先 OR 号段发号 (一次决策)
 *     2. 回收池足够 -> 批量复用
 *     3. 回收池不足 -> 走号段发号
 *   永远不混用！
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenerateService {

    private final CodeRuleRepository codeRuleRepository;
    private final CodeRuleSegmentRepository codeRuleSegmentRepository;
    private final CodeSegmentRepository codeSegmentRepository;
    private final CodeAllocationRepository codeAllocationRepository;
    private final SequenceGenerator sequenceGenerator;
    private final CodeDedupService codeDedupService;
    private final DslEngine dslEngine;
    private final GroovyScriptEngine groovyScriptEngine;
    private final WaterMarkService waterMarkService;
    private final RecycleStrategyService recycleStrategyService;
    private final PhysicalTablePublisher physicalTablePublisher;
    private final ObjectMapper objectMapper;
    private final RuleCacheService ruleCacheService;

    @Value("${codecenter.batch.max-batch-size:1000}")
    private int maxBatchSize;

    /* ====================== 单条生成 ====================== */

    @Transactional
    public CodeGenerateResponse generate(CodeGenerateRequest request) {
        long startMs = System.currentTimeMillis();
        try {
            // Step 1: 从缓存取 EFFECT 规则 + 码段快照 (命中: 0 次 DB; 未命中: 3 次)
            RuleBundle bundle = ruleCacheService.getEffectiveBundle(
                            request.getModelId(), request.getFieldId())
                    .orElseThrow(() -> BizException.ruleNotFound(
                            "model=" + request.getModelId() + ",field=" + request.getFieldId()));

            // Step 2: 构建上下文
            SegmentContext context = buildContext(request, bundle);

            // Step 3: DSL 引擎 或 Groovy 脚本
            String code = generateCodeByRule(bundle, context, request);

            // Step 4: 防重
            codeDedupService.validateAndSave(code);

            // Step 5: 持久化
            CodeAllocation allocation = persistAllocation(bundle, code, context, request);
            long elapsed = System.currentTimeMillis() - startMs;
            log.info("Code generated: code={}, seq={}, rule={}, elapsed={}ms",
                    code, context.getSequenceNum(), bundle.ruleId(), elapsed);
            return CodeGenerateResponse.success(code, context.getSequenceNum(), allocation.getId());

        } catch (BizException e) {
            log.warn("Code generate failed: {}", e.getMessage());
            return CodeGenerateResponse.fail(e.getErrorCode() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("Code generate failed: modelId={}, fieldId={}",
                    request.getModelId(), request.getFieldId(), e);
            return CodeGenerateResponse.fail("CODECENTER-CODE-3001: " + e.getMessage());
        }
    }

    /* ====================== 批量生成 ====================== */

    @Transactional
    public List<CodeGenerateResponse> batchGenerate(BatchCodeGenerateRequest request) {
        int count = request.getCount() == null ? 1 : Math.min(request.getCount(), maxBatchSize);
        if (count <= 0) {
            throw new BizException("CODECENTER-CODE-3003", "Count must be > 0");
        }

        RuleBundle bundle = ruleCacheService.getEffectiveBundle(
                        request.getModelId(), request.getFieldId())
                .orElseThrow(() -> BizException.ruleNotFound(
                        "model=" + request.getModelId() + ",field=" + request.getFieldId()));

        // V0.3 单源原则
        List<CodeAllocation> recycled = recycleStrategyService.acquireRecycled(
                bundle.ruleId(), count, LocalDateTime.now());

        if (recycled.size() == count) {
            log.info("Batch source: RECYCLE POOL, count={}", count);
            return generateFromRecyclePool(bundle, request, recycled, count);
        } else {
            log.info("Batch source: SEQUENCE, count={}", count);
            return generateFromSequence(bundle, request, count);
        }
    }

    private List<CodeGenerateResponse> generateFromRecyclePool(RuleBundle bundle, BatchCodeGenerateRequest request,
                                                              List<CodeAllocation> recycled, int count) {
        List<CodeGenerateResponse> responses = new ArrayList<>(count);
        List<CodeAllocation> toPersist = new ArrayList<>(count);

        for (CodeAllocation allocation : recycled) {
            try {
                SegmentContext ctx = buildContextForAllocation(allocation, bundle, request);
                CodeGenerateRequest single = CodeGenerateRequest.builder()
                        .modelId(request.getModelId())
                        .fieldId(request.getFieldId())
                        .dataId(request.getDataId())
                        .data(request.getData())
                        .tenantId(request.getTenantId())
                        .build();
                String code = generateCodeByRule(bundle, ctx, single);

                // Dedup check BEFORE mutating the allocation - we want the DB still in RECYCLED state
                // so the L3 check (which excludes RECYCLED/CANCELLED) passes correctly.
                codeDedupService.validateAndSave(code);

                // Now safely transition to PENDING
                allocation.setCode(code);
                allocation.setSequenceNum(ctx.getSequenceNum());
                allocation.setSegmentValues(toSegmentValuesJson(ctx));
                allocation.setStatus(AllocationStatus.PENDING);
                allocation.setCancelTime(null);
                allocation.setRecycleTime(null);
                allocation.setRecycleLockTime(null);
                allocation.setWasteType(null);
                allocation.setDataId(null);
                allocation.setConfirmTime(null);
                allocation.setUsedTime(null);
                allocation.setAllocateTime(LocalDateTime.now());
                allocation.setIsExposed(1);
                toPersist.add(allocation);
                responses.add(CodeGenerateResponse.success(code, ctx.getSequenceNum(), allocation.getId()));
            } catch (Exception e) {
                log.warn("Recycle reuse failed: {}", e.getMessage());
                responses.add(CodeGenerateResponse.fail("Recycle reuse: " + e.getMessage()));
            }
        }
        if (!toPersist.isEmpty()) {
            codeAllocationRepository.saveAll(toPersist);
        }
        return responses;
    }

    private List<CodeGenerateResponse> generateFromSequence(RuleBundle bundle, BatchCodeGenerateRequest request, int count) {
        List<CodeGenerateResponse> responses = new ArrayList<>(count);
        List<CodeAllocation> allocations = new ArrayList<>(count);
        Set<String> duplicates = new HashSet<>();

        for (int i = 0; i < count; i++) {
            try {
                CodeGenerateRequest single = CodeGenerateRequest.builder()
                        .modelId(request.getModelId())
                        .fieldId(request.getFieldId())
                        .dataId(request.getDataId())
                        .data(request.getData())
                        .tenantId(request.getTenantId())
                        .build();
                SegmentContext ctx = buildContext(single, bundle);
                String code = generateCodeByRule(bundle, ctx, single);
                try {
                    codeDedupService.validateAndSave(code);
                } catch (IllegalStateException dup) {
                    duplicates.add(code);
                    responses.add(CodeGenerateResponse.fail("Duplicate: " + code));
                    continue;
                }
                CodeAllocation allocation = persistAllocation(bundle, code, ctx, single);
                allocations.add(allocation);
                responses.add(CodeGenerateResponse.success(code, ctx.getSequenceNum(), allocation.getId()));
            } catch (Exception e) {
                log.warn("Batch sequence generate failed at index {}: {}", i, e.getMessage());
                responses.add(CodeGenerateResponse.fail(e.getMessage()));
            }
        }
        log.info("Batch from sequence: {}/{} ok, {} duplicates", allocations.size(), count, duplicates.size());
        return responses;
    }

    /* ====================== 工具方法 ====================== */

    private SegmentContext buildContext(CodeGenerateRequest request, RuleBundle bundle) {
        String baseBizTag = "MD:" + bundle.modelId() + ":" + bundle.fieldId();
        return SegmentContext.builder()
                .modelId(request.getModelId())
                .fieldId(request.getFieldId())
                .data(request.getData() == null ? new HashMap<>() : new HashMap<>(request.getData()))
                .tenantId(request.getTenantId())
                .baseBizTag(baseBizTag)
                .now(LocalDateTime.now())
                .build();
    }

    private SegmentContext buildContextForAllocation(CodeAllocation allocation, RuleBundle bundle,
                                                     BatchCodeGenerateRequest request) {
        return SegmentContext.builder()
                .modelId(bundle.modelId())
                .fieldId(bundle.fieldId())
                .data(request.getData() == null ? new HashMap<>() : new HashMap<>(request.getData()))
                .tenantId(request.getTenantId())
                .baseBizTag("MD:" + bundle.modelId() + ":" + bundle.fieldId())
                .now(LocalDateTime.now())
                .sequenceNum(allocation.getSequenceNum())
                .skipWaterMark(true)
                .build();
    }

    private String generateCodeByRule(RuleBundle bundle, SegmentContext ctx, CodeGenerateRequest req) {
        if (bundle.groovyScript() != null && !bundle.groovyScript().isEmpty()
                && bundle.ruleMode() == RuleMode.GROOVY) {
            return groovyScriptEngine.execute(bundle.groovyScript(),
                    ctx.getData() == null ? new HashMap<>() : new HashMap<>(ctx.getData()));
        }

        // DSL 模式 - 从 bundle 重建 transient 实体 (SegmentProcessor 接收 CodeSegment 实体).
        // 实体仅作数据载体使用, 不持久化, 不访问 lazy 字段.
        List<CodeRuleSegment> ruleSegments = bundle.ruleSegments().stream()
                .map(rs -> {
                    CodeRuleSegment e = new CodeRuleSegment();
                    e.setSegmentId(rs.segmentId());
                    e.setSortOrder(rs.sortOrder());
                    return e;
                })
                .sorted(Comparator.comparingInt(CodeRuleSegment::getSortOrder))
                .collect(Collectors.toList());
        Map<String, CodeSegment> segmentMap = new HashMap<>(bundle.segmentMap().size());
        for (Map.Entry<String, RuleBundle.SegmentDef> e : bundle.segmentMap().entrySet()) {
            RuleBundle.SegmentDef def = e.getValue();
            CodeSegment seg = new CodeSegment();
            seg.setId(def.id());
            seg.setSegmentCode(def.code());
            seg.setSegmentName(def.name());
            seg.setSegmentType(def.type());
            seg.setConfigJson(def.configJson());
            seg.setIsArchived(def.isArchived());
            segmentMap.put(e.getKey(), seg);
        }
        FormatTemplate template = FormatTemplate.parse(bundle.dslTemplate());
        DslEngine.DslResult result = dslEngine.execute(template, ruleSegments, segmentMap, ctx);
        return result.code();
    }

    private CodeAllocation persistAllocation(RuleBundle bundle, String code, SegmentContext ctx, CodeGenerateRequest req) {
        CodeAllocation allocation = CodeAllocation.builder()
                .id(IdUtil.simpleId())
                .ruleId(bundle.ruleId())
                .ruleVersionId(bundle.ruleId())
                .code(code)
                .sequenceNum(ctx.getSequenceNum())
                .status(AllocationStatus.PENDING)
                .isExposed(1)
                .isArchived(0)
                .dataId(req.getDataId())
                .allocateTime(LocalDateTime.now())
                .tenantId(req.getTenantId())
                .traceId(org.slf4j.MDC.get("traceId"))
                .segmentValues(toSegmentValuesJson(ctx))
                .build();
        return codeAllocationRepository.save(allocation);
    }

    private String toSegmentValuesJson(SegmentContext ctx) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("modelId", ctx.getModelId());
            map.put("fieldId", ctx.getFieldId());
            map.put("baseBizTag", ctx.getBaseBizTag());
            map.put("sequenceNum", ctx.getSequenceNum());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
