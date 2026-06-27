package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.application.dto.BatchCodeGenerateRequest;
import com.meritdata.mdm.codecenter.application.dto.CodeGenerateRequest;
import com.meritdata.mdm.codecenter.application.dto.CodeGenerateResponse;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.*;
import com.meritdata.mdm.codecenter.domain.enums.*;
import com.meritdata.mdm.codecenter.domain.repository.*;
import com.meritdata.mdm.codecenter.infrastructure.dedup.CodeDedupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CodeGenerateServiceIntegrationTest {

    @Autowired private CodeGenerateService codeGenerateService;
    @Autowired private CodeRuleRepository codeRuleRepository;
    @Autowired private CodeRuleSegmentRepository codeRuleSegmentRepository;
    @Autowired private CodeSegmentRepository codeSegmentRepository;
    @Autowired private CodeAllocationRepository codeAllocationRepository;
    @Autowired private ThemeDomainRepository themeDomainRepository;
    @Autowired private ModelRepository modelRepository;
    @Autowired private ModelAttributeRepository modelAttributeRepository;
    @Autowired private CodeWaterMarkRepository codeWaterMarkRepository;
    @Autowired private ModelAuditLogRepository modelAuditLogRepository;
    @Autowired private CodeDedupService codeDedupService;

    private String modelId;
    private String fieldId;

    @BeforeEach
    @Transactional
    @Commit
    void setup() {
        codeAllocationRepository.deleteAll();
        codeWaterMarkRepository.deleteAll();
        codeRuleSegmentRepository.deleteAll();
        codeRuleRepository.deleteAll();
        codeSegmentRepository.deleteAll();
        modelAuditLogRepository.deleteAll();
        modelAttributeRepository.deleteAll();
        modelRepository.deleteAll();
        themeDomainRepository.deleteAll();

        String tenant = "test-tenant-" + IdUtil.shortId();

        ThemeDomain theme = themeDomainRepository.save(ThemeDomain.builder()
                .id(IdUtil.simpleId())
                .domainCode("T_TEST_" + IdUtil.shortId())
                .domainName("Test Theme")
                .sortOrder(1)
                .tenantId(tenant)
                .createBy("tester")
                .build());

        Model model = modelRepository.save(Model.builder()
                .id(IdUtil.simpleId())
                .modelCode("MD_TEST_" + IdUtil.shortId())
                .modelName("Test Model")
                .tableName("t_test_" + IdUtil.shortId())
                .modelType(ModelType.NORMAL)
                .themeId(theme.getId())
                .securityLevel("INTERNAL")
                .version(1)
                .status(ModelStatus.EFFECT)
                .tenantId(tenant)
                .createBy("tester")
                .build());
        modelId = model.getId();

        ModelAttribute attr = modelAttributeRepository.save(ModelAttribute.builder()
                .id(IdUtil.simpleId())
                .modelId(modelId)
                .cnName("code")
                .enName("code")
                .dataType("STRING")
                .dataLength(64)
                .isRequired(true)
                .isUnique(true)
                .isCodeField(true)
                .sortOrder(1)
                .status("EFFECT")
                .tenantId(tenant)
                .build());
        fieldId = attr.getId();
    }

    @Test
    @Order(1)
    void singleGenerationProducesExpectedCode() {
        String bizTag = "MD:WL_TEST:" + IdUtil.shortId();
        CodeSegment fixSeg = saveSegment("FIX_MAT", SegmentType.FIXED, "{\"value\":\"MAT\"}");
        CodeSegment dateSeg = saveSegment("DATE_YYYYMMDD", SegmentType.DATE, "{\"format\":\"yyyyMMdd\"}");
        CodeSegment seqSeg = saveSegment("SEQ_6", SegmentType.SEQUENCE,
                "{\"length\":6,\"bizTag\":\"" + bizTag + "\"}");

        CodeRule rule = saveRule(bizTag + "_RULE", "{FIXED}-{DATE}-{SEQUENCE}");
        linkRuleSegment(rule.getId(), fixSeg.getId(), 0);
        linkRuleSegment(rule.getId(), dateSeg.getId(), 1);
        linkRuleSegment(rule.getId(), seqSeg.getId(), 2);

        CodeGenerateResponse response = codeGenerateService.generate(
                CodeGenerateRequest.builder()
                        .modelId(modelId)
                        .fieldId(fieldId)
                        .tenantId(rule.getTenantId())
                        .build());

        assertNotNull(response);
        assertTrue(Boolean.TRUE.equals(response.getSuccess()),
                "Generation should succeed: " + response.getErrorMessage());
        assertNotNull(response.getCode());
        assertTrue(response.getCode().startsWith("MAT-"), "Code should start with MAT-: " + response.getCode());
        assertTrue(response.getCode().contains("-000001"), "Code should contain -000001: " + response.getCode());
    }

    @Test
    @Order(2)
    void batchGenerationProducesUniqueCodes() {
        String bizTag = "MD:WL_TEST:BATCH:" + IdUtil.shortId();
        CodeSegment fixSeg = saveSegment("FIX_BL", SegmentType.FIXED, "{\"value\":\"BL\"}");
        CodeSegment seqSeg = saveSegment("SEQ_4_BATCH", SegmentType.SEQUENCE,
                "{\"length\":4,\"bizTag\":\"" + bizTag + "\"}");

        CodeRule rule = saveRule(bizTag + "_RULE", "{FIXED}-{SEQUENCE}");
        linkRuleSegment(rule.getId(), fixSeg.getId(), 0);
        linkRuleSegment(rule.getId(), seqSeg.getId(), 1);

        List<CodeGenerateResponse> responses = codeGenerateService.batchGenerate(
                BatchCodeGenerateRequest.builder()
                        .modelId(modelId)
                        .fieldId(fieldId)
                        .tenantId(rule.getTenantId())
                        .count(20)
                        .build());

        long successCount = responses.stream()
                .filter(r -> Boolean.TRUE.equals(r.getSuccess())).count();
        assertEquals(20, successCount, "All 20 should succeed");

        Set<String> uniqueCodes = responses.stream()
                .map(CodeGenerateResponse::getCode)
                .collect(Collectors.toSet());
        assertEquals(20, uniqueCodes.size(), "All codes must be unique");

        assertEquals(20, codeAllocationRepository.findByRuleId(rule.getId(), Pageable.unpaged())
                .getNumberOfElements());
    }

    @Test
    @Order(3)
    @Transactional
    @Commit
    void recyclePoolReuseOnSecondBatch() {
        String bizTag = "MD:WL_TEST:RECYCLE:" + IdUtil.shortId();
        CodeSegment fixSeg = saveSegment("FIX_RP", SegmentType.FIXED, "{\"value\":\"RP\"}");
        CodeSegment seqSeg = saveSegment("SEQ_4_RP", SegmentType.SEQUENCE,
                "{\"length\":4,\"bizTag\":\"" + bizTag + "\"}");

        CodeRule rule = saveRule(bizTag + "_RULE", "{FIXED}-{SEQUENCE}");
        linkRuleSegment(rule.getId(), fixSeg.getId(), 0);
        linkRuleSegment(rule.getId(), seqSeg.getId(), 1);

        List<CodeGenerateResponse> first = codeGenerateService.batchGenerate(
                BatchCodeGenerateRequest.builder()
                        .modelId(modelId).fieldId(fieldId).tenantId(rule.getTenantId()).count(5).build());
        assertEquals(5, first.stream().filter(r -> Boolean.TRUE.equals(r.getSuccess())).count());

        // Release codes from L1 cache and move to RECYCLED with past lock time
        for (CodeGenerateResponse r : first) {
            if (Boolean.TRUE.equals(r.getSuccess())) {
                codeDedupService.release(r.getCode());
            }
        }

        // Mark all as RECYCLED with past lock time
        markRecycled(rule.getId());

        List<CodeGenerateResponse> second = codeGenerateService.batchGenerate(
                BatchCodeGenerateRequest.builder()
                        .modelId(modelId).fieldId(fieldId).tenantId(rule.getTenantId()).count(5).build());
        long secondSuccess = second.stream().filter(r -> Boolean.TRUE.equals(r.getSuccess())).count();
        assertEquals(5, secondSuccess, "Recycle pool should satisfy the batch");

        Set<String> firstCodes = first.stream()
                .map(CodeGenerateResponse::getCode).collect(Collectors.toSet());
        Set<String> secondCodes = second.stream()
                .map(CodeGenerateResponse::getCode).collect(Collectors.toSet());
        assertEquals(firstCodes, secondCodes, "Recycle pool should return the same codes");
    }

    @Transactional
    @Commit
    void markRecycled(String ruleId) {
        List<CodeAllocation> all = codeAllocationRepository.findByRuleId(ruleId, Pageable.unpaged())
                .getContent();
        for (CodeAllocation a : all) {
            a.setStatus(AllocationStatus.RECYCLED);
            a.setRecycleLockTime(java.time.LocalDateTime.now().minusHours(1));
        }
        codeAllocationRepository.saveAll(all);
        codeAllocationRepository.flush();
    }

    private CodeSegment saveSegment(String codePrefix, SegmentType type, String configJson) {
        return codeSegmentRepository.save(CodeSegment.builder()
                .id(IdUtil.simpleId())
                .segmentCode(codePrefix + "_" + IdUtil.shortId())
                .segmentName(codePrefix)
                .segmentType(type)
                .configJson(configJson)
                .isArchived(false)
                .tenantId("test-tenant")
                .createdBy("tester")
                .build());
    }

    private CodeRule saveRule(String ruleCode, String dslTemplate) {
        return codeRuleRepository.save(CodeRule.builder()
                .id(IdUtil.simpleId())
                .modelId(modelId)
                .encodeFieldId(fieldId)
                .ruleName("rule " + ruleCode)
                .ruleCode(ruleCode)
                .ruleMode(RuleMode.DSL)
                .triggerType(GenerateTrigger.BUTTON)
                .dslTemplate(dslTemplate)
                .version(1)
                .status(RuleStatus.EFFECT)
                .recycleLockHours(24)
                .recycleStrategy("AUTO")
                .tenantId("test-tenant")
                .createdBy("tester")
                .build());
    }

    private void linkRuleSegment(String ruleId, String segmentId, int order) {
        codeRuleSegmentRepository.save(CodeRuleSegment.builder()
                .id(IdUtil.simpleId())
                .ruleId(ruleId)
                .segmentId(segmentId)
                .sortOrder(order)
                .tenantId("test-tenant")
                .build());
    }
}
