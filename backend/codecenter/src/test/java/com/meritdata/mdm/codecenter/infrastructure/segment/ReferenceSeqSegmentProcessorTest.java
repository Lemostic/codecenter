package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.application.service.WaterMarkService;
import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.infrastructure.sequence.SequenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

class ReferenceSeqSegmentProcessorTest {

    private SequenceGenerator generator;
    private WaterMarkService waterMark;
    private ReferenceSeqSegmentProcessor processor;
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
        // V0.5: claimNext 透传 requestedSeq
        Mockito.when(waterMark.claimNext(anyString(), anyLong()))
                .thenAnswer(inv -> inv.getArgument(1));

        processor = new ReferenceSeqSegmentProcessor(generator, waterMark);
    }

    @Test
    void supportedTypeIsReferenceSeq() {
        assertEquals(SegmentType.REFERENCE_SEQ, processor.supportedType());
    }

    @Test
    void differentRefValueIndependentSequence() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("RS")
                .segmentType(SegmentType.REFERENCE_SEQ)
                .configJson("{\"refField\":\"contractCode\",\"length\":4,\"bizTagPrefix\":\"MD:CONTRACT\"}")
                .build();

        Map<String, Object> dataA = new HashMap<>();
        dataA.put("contractCode", "A");
        Map<String, Object> dataB = new HashMap<>();
        dataB.put("contractCode", "B");

        SegmentContext ctxA = SegmentContext.builder()
                .data(dataA).now(LocalDateTime.now()).build();
        SegmentContext ctxB = SegmentContext.builder()
                .data(dataB).now(LocalDateTime.now()).build();

        SegmentResult rA1 = processor.process(seg, ctxA);
        SegmentResult rB1 = processor.process(seg, ctxB);
        SegmentResult rA2 = processor.process(seg, ctxA);

        assertEquals("0001", rA1.getSegmentValue());
        assertEquals("0001", rB1.getSegmentValue());
        assertEquals("0002", rA2.getSegmentValue());
        assertTrue(rA1.getSequenceBizTag().endsWith(":A"));
        assertTrue(rB1.getSequenceBizTag().endsWith(":B"));
    }

    @Test
    void missingRefFieldThrows() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("RS")
                .segmentType(SegmentType.REFERENCE_SEQ)
                .configJson("{\"length\":4,\"bizTagPrefix\":\"MD:CONTRACT\"}")
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("contractCode", "A");
        assertThrows(IllegalArgumentException.class, () ->
                processor.process(seg, SegmentContext.builder().data(data).build()));
    }

    @Test
    void nullRefValueThrows() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("RS")
                .segmentType(SegmentType.REFERENCE_SEQ)
                .configJson("{\"refField\":\"contractCode\",\"length\":4}")
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("contractCode", null);
        assertThrows(IllegalArgumentException.class, () ->
                processor.process(seg, SegmentContext.builder().data(data).build()));
    }
}
