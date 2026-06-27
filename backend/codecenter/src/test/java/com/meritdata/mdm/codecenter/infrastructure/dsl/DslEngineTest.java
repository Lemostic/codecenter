package com.meritdata.mdm.codecenter.infrastructure.dsl;

import com.meritdata.mdm.codecenter.application.service.WaterMarkService;
import com.meritdata.mdm.codecenter.domain.entity.CodeRuleSegment;
import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.domain.valueobject.FormatTemplate;
import com.meritdata.mdm.codecenter.infrastructure.segment.*;
import com.meritdata.mdm.codecenter.infrastructure.sequence.SequenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

class DslEngineTest {

    private SegmentProcessorRegistry registry;
    private DslEngine dslEngine;
    private SequenceGenerator generator;
    private WaterMarkService waterMark;
    private ConcurrentHashMap<String, AtomicLong> counters;

    @BeforeEach
    void setup() {
        generator = Mockito.mock(SequenceGenerator.class);
        waterMark = Mockito.mock(WaterMarkService.class);
        counters = new ConcurrentHashMap<>();
        Mockito.when(generator.nextSequence(anyString(), anyInt())).thenAnswer(inv -> {
            String tag = inv.getArgument(0);
            return counters.computeIfAbsent(tag, k -> new AtomicLong(0)).incrementAndGet();
        });
        Mockito.when(waterMark.claimNext(anyString(), anyLong())).thenAnswer(inv -> inv.getArgument(1));
        

        List<SegmentProcessor> processors = List.of(
                new FixedSegmentProcessor(),
                new DateSegmentProcessor(),
                new SequenceSegmentProcessor(generator, waterMark),
                new EigenvalueSegmentProcessor(),
                new ReferenceSegmentProcessor(),
                new ReferenceSeqSegmentProcessor(generator, waterMark)
        );

        registry = new SegmentProcessorRegistry(processors);
        registry.init();

        dslEngine = new DslEngine(registry);
    }

    @Test
    void executesAllSixSegmentTypes() {
        FormatTemplate tpl = FormatTemplate.parse(
                "{FIXED:value=PR}-{DATE:format=yyyyMMdd}-{EIGENVALUE:sourceField=cat;defaultValue=OT}"
                        + "-{REFERENCE:associatedAttribute=contract;cutStep=3}"
                        + "-{SEQUENCE:length=4;bizTag=MD:WL}"
                        + "-{REFERENCE_SEQ:refField=contract;length=4}");

        Map<String, Object> data = new HashMap<>();
        data.put("cat", "raw-material");
        data.put("contract", "ABC-2025-001");

        SegmentContext ctx = SegmentContext.builder()
                .data(data)
                .now(LocalDateTime.of(2026, 6, 27, 10, 0, 0))
                .modelId("M_TEST")
                .fieldId("F_CODE")
                .build();

        List<CodeRuleSegment> ruleSegments = List.of(
                rs(0, "FIX"),
                rs(1, "DT"),
                rs(2, "EV"),
                rs(3, "REF"),
                rs(4, "SEQ"),
                rs(5, "RS")
        );
        Map<String, CodeSegment> segmentMap = new HashMap<>();
        segmentMap.put("FIX", seg(SegmentType.FIXED, "FIX", "{\"value\":\"PR\"}"));
        segmentMap.put("DT", seg(SegmentType.DATE, "DT", "{\"format\":\"yyyyMMdd\"}"));
        segmentMap.put("EV", seg(SegmentType.EIGENVALUE, "EV", "{\"sourceField\":\"cat\",\"defaultValue\":\"OT\"}"));
        segmentMap.put("REF", seg(SegmentType.REFERENCE, "REF", "{\"associatedAttribute\":\"contract\",\"cutStep\":3}"));
        segmentMap.put("SEQ", seg(SegmentType.SEQUENCE, "SEQ", "{\"length\":4,\"bizTag\":\"MD:WL\"}"));
        segmentMap.put("RS", seg(SegmentType.REFERENCE_SEQ, "RS", "{\"refField\":\"contract\",\"length\":4}"));

        DslEngine.DslResult result = dslEngine.execute(tpl, ruleSegments, segmentMap, ctx);

        assertEquals("PR-20260627-OT-ABC-0001-0001", result.code());
        assertEquals(6, result.segmentResults().size());
    }

    @Test
    void literalTextIsPreserved() {
        FormatTemplate tpl = FormatTemplate.parse("PRE-{FIXED:value=AB}-POST");
        SegmentContext ctx = SegmentContext.builder().build();
        List<CodeRuleSegment> ruleSegments = List.of(rs(0, "FX"));
        Map<String, CodeSegment> segmentMap = new HashMap<>();
        segmentMap.put("FX", seg(SegmentType.FIXED, "FX", "{\"value\":\"AB\"}"));

        DslEngine.DslResult result = dslEngine.execute(tpl, ruleSegments, segmentMap, ctx);
        assertEquals("PRE-AB-POST", result.code());
    }

    @Test
    void tooManyTokensThrows() {
        FormatTemplate tpl = FormatTemplate.parse("{FIXED:value=A}{FIXED:value=B}");
        SegmentContext ctx = SegmentContext.builder().build();
        List<CodeRuleSegment> ruleSegments = List.of(rs(0, "FX"));
        Map<String, CodeSegment> segmentMap = new HashMap<>();
        segmentMap.put("FX", seg(SegmentType.FIXED, "FX", "{\"value\":\"A\"}"));

        assertThrows(IllegalStateException.class, () ->
                dslEngine.execute(tpl, ruleSegments, segmentMap, ctx));
    }

    private CodeRuleSegment rs(int order, String segId) {
        return CodeRuleSegment.builder()
                .id(segId + "-rs")
                .ruleId("R1")
                .segmentId(segId)
                .sortOrder(order)
                .build();
    }

    private CodeSegment seg(SegmentType type, String code, String config) {
        return CodeSegment.builder()
                .id(code)
                .segmentCode(code)
                .segmentName(code)
                .segmentType(type)
                .configJson(config)
                .isArchived(false)
                .build();
    }
}
