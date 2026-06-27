package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EigenvalueSegmentProcessorTest {

    private final EigenvalueSegmentProcessor processor = new EigenvalueSegmentProcessor();

    @Test
    void supportedTypeIsEigenvalue() {
        assertEquals(SegmentType.EIGENVALUE, processor.supportedType());
    }

    @Test
    void mappedValueReturned() {
        String config = "{\"sourceField\":\"category\",\"mappingTable\":{\"原材料\":\"MC\",\"成品\":\"FP\"},\"defaultValue\":\"OT\"}";
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("EV")
                .segmentType(SegmentType.EIGENVALUE)
                .configJson(config)
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("category", "成品");
        SegmentContext ctx = SegmentContext.builder().data(data).build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("FP", r.getSegmentValue());
    }

    @Test
    void missingValueFallsBackToDefault() {
        String config = "{\"sourceField\":\"category\",\"mappingTable\":{\"原材料\":\"MC\"},\"defaultValue\":\"OT\"}";
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("EV")
                .segmentType(SegmentType.EIGENVALUE)
                .configJson(config)
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("category", "未知");
        SegmentContext ctx = SegmentContext.builder().data(data).build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("OT", r.getSegmentValue());
    }

    @Test
    void nullValueFallsBackToDefault() {
        String config = "{\"sourceField\":\"category\",\"defaultValue\":\"DF\"}";
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("EV")
                .segmentType(SegmentType.EIGENVALUE)
                .configJson(config)
                .build();
        SegmentContext ctx = SegmentContext.builder().data(new HashMap<>()).build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("DF", r.getSegmentValue());
    }

    @Test
    void missingSourceFieldThrows() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("EV")
                .segmentType(SegmentType.EIGENVALUE)
                .configJson("{\"defaultValue\":\"DF\"}")
                .build();
        assertThrows(IllegalArgumentException.class, () ->
                processor.process(seg, SegmentContext.builder().build()));
    }
}
