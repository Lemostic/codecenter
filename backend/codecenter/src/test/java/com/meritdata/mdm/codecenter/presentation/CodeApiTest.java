package com.meritdata.mdm.codecenter.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meritdata.mdm.codecenter.application.dto.BatchCodeGenerateRequest;
import com.meritdata.mdm.codecenter.application.dto.CodeGenerateRequest;
import com.meritdata.mdm.codecenter.application.service.CodeGenerateService;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.*;
import com.meritdata.mdm.codecenter.domain.enums.*;
import com.meritdata.mdm.codecenter.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API end-to-end test - exercises CodeController through MockMvc.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CodeApiTest {

    @Autowired private WebApplicationContext webContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CodeRuleRepository codeRuleRepository;
    @Autowired private CodeSegmentRepository codeSegmentRepository;
    @Autowired private CodeRuleSegmentRepository codeRuleSegmentRepository;
    @Autowired private CodeAllocationRepository codeAllocationRepository;
    @Autowired private ModelRepository modelRepository;
    @Autowired private ModelAttributeRepository modelAttributeRepository;
    @Autowired private ThemeDomainRepository themeDomainRepository;
    @Autowired private CodeWaterMarkRepository codeWaterMarkRepository;
    @Autowired private CodeGenerateService codeGenerateService;
    @Autowired private com.meritdata.mdm.codecenter.infrastructure.dedup.CodeDedupService codeDedupService;

    private MockMvc mockMvc;
    private String modelId;
    private String fieldId;
    private String tenantId;

    @BeforeEach
    @Transactional
    @Commit
    void setup() {
        codeAllocationRepository.deleteAll();
        codeWaterMarkRepository.deleteAll();
        codeRuleSegmentRepository.deleteAll();
        codeRuleRepository.deleteAll();
        codeSegmentRepository.deleteAll();
        modelAttributeRepository.deleteAll();
        modelRepository.deleteAll();
        themeDomainRepository.deleteAll();

        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
        codeDedupService.clearMemoryCache();
        tenantId = "test-tenant-" + IdUtil.shortId();

        ThemeDomain theme = themeDomainRepository.save(ThemeDomain.builder()
                .id(IdUtil.simpleId())
                .domainCode("T_API_" + IdUtil.shortId())
                .domainName("API Test Theme")
                .sortOrder(1)
                .tenantId(tenantId)
                .createBy("tester")
                .build());

        Model model = modelRepository.save(Model.builder()
                .id(IdUtil.simpleId())
                .modelCode("MD_API_" + IdUtil.shortId())
                .modelName("API Test Model")
                .tableName("t_api_" + IdUtil.shortId())
                .modelType(ModelType.NORMAL)
                .themeId(theme.getId())
                .securityLevel("INTERNAL")
                .version(1)
                .status(ModelStatus.EFFECT)
                .tenantId(tenantId)
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
                .tenantId(tenantId)
                .build());
        fieldId = attr.getId();

        String bizTag = "MD:API:" + IdUtil.shortId();
        CodeSegment fixSeg = codeSegmentRepository.save(CodeSegment.builder()
                .id(IdUtil.simpleId())
                .segmentCode("FIX_API")
                .segmentName("FIX_API")
                .segmentType(SegmentType.FIXED)
                .configJson("{\"value\":\"API\"}")
                .isArchived(false)
                .tenantId(tenantId)
                .createdBy("tester")
                .build());
        CodeSegment seqSeg = codeSegmentRepository.save(CodeSegment.builder()
                .id(IdUtil.simpleId())
                .segmentCode("SEQ_API")
                .segmentName("SEQ_API")
                .segmentType(SegmentType.SEQUENCE)
                .configJson("{\"length\":4,\"bizTag\":\"" + bizTag + "\"}")
                .isArchived(false)
                .tenantId(tenantId)
                .createdBy("tester")
                .build());

        CodeRule rule = codeRuleRepository.save(CodeRule.builder()
                .id(IdUtil.simpleId())
                .modelId(modelId)
                .encodeFieldId(fieldId)
                .ruleName("API rule")
                .ruleCode(bizTag + "_RULE")
                .ruleMode(RuleMode.DSL)
                .triggerType(GenerateTrigger.BUTTON)
                .dslTemplate("{FIXED}-{SEQUENCE}")
                .version(1)
                .status(RuleStatus.EFFECT)
                .recycleLockHours(24)
                .recycleStrategy("AUTO")
                .tenantId(tenantId)
                .createdBy("tester")
                .build());

        codeRuleSegmentRepository.save(CodeRuleSegment.builder()
                .id(IdUtil.simpleId())
                .ruleId(rule.getId())
                .segmentId(fixSeg.getId())
                .sortOrder(0)
                .tenantId(tenantId)
                .build());
        codeRuleSegmentRepository.save(CodeRuleSegment.builder()
                .id(IdUtil.simpleId())
                .ruleId(rule.getId())
                .segmentId(seqSeg.getId())
                .sortOrder(1)
                .tenantId(tenantId)
                .build());
    }

    @Test
    @Order(1)
    void generateEndpointReturnsCode() throws Exception {
        CodeGenerateRequest request = CodeGenerateRequest.builder()
                .modelId(modelId)
                .fieldId(fieldId)
                .tenantId(tenantId)
                .build();
        MvcResult result = mockMvc.perform(post("/api/mdm/encode/codes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").exists())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        String code = root.path("data").path("code").asText();
        assertNotNull(code);
        assertTrue(code.startsWith("API-"), "Code should start with API-: " + code);
    }

    @Test
    @Order(2)
    void batchGenerateEndpointReturnsList() throws Exception {
        BatchCodeGenerateRequest request = BatchCodeGenerateRequest.builder()
                .modelId(modelId)
                .fieldId(fieldId)
                .tenantId(tenantId)
                .count(5)
                .build();
        MvcResult result = mockMvc.perform(post("/api/mdm/encode/codes/batch-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.path("data");
        Set<String> codes = new HashSet<>();
        for (Iterator<JsonNode> it = data.elements(); it.hasNext(); ) {
            JsonNode elem = it.next();
            if (elem.path("success").asBoolean(false)) {
                codes.add(elem.path("code").asText());
            }
        }
        assertEquals(5, codes.size(), "All 5 codes should be unique");
    }

    @Test
    @Order(3)
    void confirmEndpointMarksCodeUsed() throws Exception {
        // generate
        CodeGenerateRequest genReq = CodeGenerateRequest.builder()
                .modelId(modelId).fieldId(fieldId).tenantId(tenantId).build();
        MvcResult genResult = mockMvc.perform(post("/api/mdm/encode/codes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(genResult.getResponse().getContentAsString());
        String code = root.path("data").path("code").asText();
        String allocId = codeAllocationRepository.findByCode(code).orElseThrow().getId();

        // confirm
        mockMvc.perform(post("/api/mdm/encode/codes/" + allocId + "/confirm")
                        .param("code", code))
                .andExpect(status().isOk());

        CodeAllocation confirmed = codeAllocationRepository.findByCode(code).orElseThrow();
        assertEquals(AllocationStatus.USED, confirmed.getStatus());
    }

    @Test
    @Order(4)
    void cancelEndpointMarksCodeCancelled() throws Exception {
        CodeGenerateRequest genReq = CodeGenerateRequest.builder()
                .modelId(modelId).fieldId(fieldId).tenantId(tenantId).build();
        MvcResult genResult = mockMvc.perform(post("/api/mdm/encode/codes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(genResult.getResponse().getContentAsString());
        String code = root.path("data").path("code").asText();
        String allocId = codeAllocationRepository.findByCode(code).orElseThrow().getId();

        mockMvc.perform(post("/api/mdm/encode/codes/" + allocId + "/cancel")
                        .param("code", code)
                        .param("wasteType", "CANCEL"))
                .andExpect(status().isOk());

        CodeAllocation cancelled = codeAllocationRepository.findByCode(code).orElseThrow();
        assertEquals(AllocationStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @Order(5)
    void waterMarkEndpointReturnsWater() throws Exception {
        mockMvc.perform(get("/api/mdm/encode/codes/water-mark/MD:API:test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bizTag").value("MD:API:test"));
    }

    @Test
    @Order(6)
    void timeoutScanEndpointRuns() throws Exception {
        mockMvc.perform(post("/api/mdm/encode/codes/timeout-scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recycled").exists());
    }

    @Test
    @Order(7)
    void generateWithMissingFieldsReturnsError() throws Exception {
        CodeGenerateRequest bad = CodeGenerateRequest.builder().build();
        MvcResult result = mockMvc.perform(post("/api/mdm/encode/codes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        // The service swallows exceptions and returns success=false in the data
        assertFalse(root.path("success").asBoolean(false),
                "Empty request should result in overall failure: " + root.toString());
    }
}
