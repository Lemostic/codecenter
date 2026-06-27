package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DateSegmentProcessorTest {

    private final DateSegmentProcessor processor = new DateSegmentProcessor();

    @Test
    void supportedTypeIsDate() {
        assertEquals(SegmentType.DATE, processor.supportedType());
    }

    @Test
    void systemDateDefaultFormat() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("DT")
                .segmentType(SegmentType.DATE)
                .configJson("{}")
                .build();
        SegmentContext ctx = SegmentContext.builder()
                .now(LocalDateTime.of(2026, 6, 27, 10, 30, 0))
                .build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("20260627", r.getSegmentValue());
    }

    @Test
    void systemDateCustomFormat() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("YR")
                .segmentType(SegmentType.DATE)
                .configJson("{\"format\":\"yyyy\"}")
                .build();
        SegmentContext ctx = SegmentContext.builder()
                .now(LocalDateTime.of(2026, 6, 27, 10, 30, 0))
                .build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("2026", r.getSegmentValue());
    }

    @Test
    void attributeDate() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("DT")
                .segmentType(SegmentType.DATE)
                .configJson("{\"format\":\"yyyyMMdd\",\"dateSource\":\"ATTRIBUTE\",\"sourceField\":\"createDate\"}")
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("createDate", LocalDate.of(2025, 12, 1));
        SegmentContext ctx = SegmentContext.builder().data(data).build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("20251201", r.getSegmentValue());
    }

    @Test
    void attributeDateString() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("DT")
                .segmentType(SegmentType.DATE)
                .configJson("{\"format\":\"yyyy/MM/dd\",\"dateSource\":\"ATTRIBUTE\",\"sourceField\":\"createdAt\"}")
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("createdAt", "2025-03-15T10:00:00");
        SegmentContext ctx = SegmentContext.builder().data(data).build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("2025/03/15", r.getSegmentValue());
    }
}
