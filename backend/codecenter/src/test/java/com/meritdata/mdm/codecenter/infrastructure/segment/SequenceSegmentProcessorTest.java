package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.application.service.WaterMarkService;
import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.infrastructure.sequence.SequenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

class SequenceSegmentProcessorTest {

    private SequenceGenerator generator;
    private WaterMarkService waterMark;
    private SequenceSegmentProcessor processor;

    @BeforeEach
    void setup() {
        generator = Mockito.mock(SequenceGenerator.class);
        waterMark = Mockito.mock(WaterMarkService.class);
        AtomicLong counter = new AtomicLong();
        Mockito.when(generator.nextSequence(anyString(), anyInt()))
                .thenAnswer(inv -> counter.incrementAndGet());
        Mockito.doNothing().when(generator).ensureBizTypeInitialized(anyString(), anyInt());
        // V0.5: claimNext 是幂等透传: 传入什么 requestedSeq, 就返回什么
        Mockito.when(waterMark.claimNext(anyString(), anyLong()))
                .thenAnswer(inv -> inv.getArgument(1));

        processor = new SequenceSegmentProcessor(generator, waterMark);
    }

    @Test
    void supportedTypeIsSequence() {
        assertEquals(SegmentType.SEQUENCE, processor.supportedType());
    }

    @Test
    void sequencePaddedToConfiguredLength() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("SEQ")
                .segmentType(SegmentType.SEQUENCE)
                .configJson("{\"length\":6,\"bizTag\":\"MD:WL:CODE\"}")
                .build();
        SegmentContext ctx = SegmentContext.builder().now(LocalDateTime.now()).build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("000001", r.getSegmentValue());
        assertEquals(1L, r.getSequenceNum());
        assertEquals("MD:WL:CODE", r.getSequenceBizTag());
    }

    @Test
    void dailyResetAppendsDateSuffix() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("SEQ")
                .segmentType(SegmentType.SEQUENCE)
                .configJson("{\"length\":4,\"bizTag\":\"MD:ORDER\",\"reset\":\"DAILY\"}")
                .build();
        SegmentContext ctx = SegmentContext.builder()
                .now(LocalDateTime.of(2026, 6, 27, 10, 30, 0))
                .build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals("0001", r.getSegmentValue());
        assertTrue(r.getSequenceBizTag().contains("D2026-06-27"));
    }

    @Test
    void monthlyResetAppendsMonthSuffix() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("SEQ")
                .segmentType(SegmentType.SEQUENCE)
                .configJson("{\"length\":4,\"bizTag\":\"MD:ORDER\",\"reset\":\"MONTHLY\"}")
                .build();
        SegmentContext ctx = SegmentContext.builder()
                .now(LocalDateTime.of(2026, 3, 5, 10, 30, 0))
                .build();

        SegmentResult r = processor.process(seg, ctx);
        assertTrue(r.getSequenceBizTag().contains("M202603"));
    }

    @Test
    void usesContextSequenceWhenProvided() {
        CodeSegment seg = CodeSegment.builder()
                .segmentCode("SEQ")
                .segmentType(SegmentType.SEQUENCE)
                .configJson("{\"length\":6,\"bizTag\":\"MD:WL\"}")
                .build();
        SegmentContext ctx = SegmentContext.builder()
                .sequenceNum(42L)
                .now(LocalDateTime.now())
                .build();

        SegmentResult r = processor.process(seg, ctx);
        assertEquals(42L, r.getSequenceNum());
        assertEquals("000042", r.getSegmentValue());
        Mockito.verify(generator, Mockito.never()).nextSequence(anyString(), anyInt());
    }
}
