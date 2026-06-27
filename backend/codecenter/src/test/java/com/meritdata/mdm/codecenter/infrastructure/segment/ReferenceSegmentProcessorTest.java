package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceSegmentProcessorTest {

    private final ReferenceSegmentProcessor processor = new ReferenceSegmentProcessor();

    @Test
    void supportedTypeIsReference() {
        assertEquals(SegmentType.REFERENCE, processor.supportedType());
    }

    @Test
    void leftDirection() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("REF")
                .segmentType(SegmentType.REFERENCE)
                .configJson("{\"associatedAttribute\":\"contractCode\",\"cutDirection\":\"LEFT\",\"cutStep\":3}")
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("contractCode", "ABC-2025-001");
        SegmentContext ctx = SegmentContext.builder().data(data).build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("ABC", r.getSegmentValue());
    }

    @Test
    void rightDirection() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("REF")
                .segmentType(SegmentType.REFERENCE)
                .configJson("{\"associatedAttribute\":\"contractCode\",\"cutDirection\":\"RIGHT\",\"cutStep\":3}")
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("contractCode", "ABC-2025-001");
        SegmentContext ctx = SegmentContext.builder().data(data).build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("001", r.getSegmentValue());
    }

    @Test
    void middleDirection() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("REF")
                .segmentType(SegmentType.REFERENCE)
                .configJson("{\"associatedAttribute\":\"contractCode\",\"cutDirection\":\"MIDDLE\",\"cutStep\":4,\"startPosition\":4}")
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("contractCode", "ABC-2025-001");
        SegmentContext ctx = SegmentContext.builder().data(data).build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("2025", r.getSegmentValue());
    }

    @Test
    void fullValueWhenStepZero() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("REF")
                .segmentType(SegmentType.REFERENCE)
                .configJson("{\"associatedAttribute\":\"contractCode\",\"cutStep\":0}")
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("contractCode", "ABC-2025-001");
        SegmentContext ctx = SegmentContext.builder().data(data).build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("ABC-2025-001", r.getSegmentValue());
    }

    @Test
    void missingRefValueThrows() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("REF")
                .segmentType(SegmentType.REFERENCE)
                .configJson("{\"associatedAttribute\":\"contractCode\"}")
                .build();
        assertThrows(IllegalArgumentException.class, () ->
                processor.process(seg, SegmentContext.builder().data(new HashMap<>()).build()));
    }
}
