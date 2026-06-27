package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixedSegmentProcessorTest {

    private final FixedSegmentProcessor processor = new FixedSegmentProcessor();

    @Test
    void supportedTypeIsFixed() {
        assertEquals(SegmentType.FIXED, processor.supportedType());
    }

    @Test
    void plainValue() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("PRE")
                .segmentType(SegmentType.FIXED)
                .configJson("{\"value\":\"WL\"}")
                .build();
        SegmentContext ctx = SegmentContext.builder().build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("WL", r.getSegmentValue());
        assertEquals(SegmentType.FIXED, r.getSegmentType());
        assertEquals("PRE", r.getSegmentCode());
    }

    @Test
    void prefixAndSuffix() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("PRE")
                .segmentType(SegmentType.FIXED)
                .configJson("{\"value\":\"PR\",\"prefix\":\"WL-\",\"suffix\":\"-END\"}")
                .build();

        SegmentResult r = processor.process(seg, SegmentContext.builder().build());
        assertEquals("WL-PR-END", r.getSegmentValue());
    }

    @Test
    void missingValueAllowed() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("DASH")
                .segmentType(SegmentType.FIXED)
                .configJson("{\"prefix\":\"--\",\"value\":null,\"suffix\":null}")
                .build();
        SegmentResult r = processor.process(seg, SegmentContext.builder().build());
        assertEquals("--", r.getSegmentValue());
    }
}
