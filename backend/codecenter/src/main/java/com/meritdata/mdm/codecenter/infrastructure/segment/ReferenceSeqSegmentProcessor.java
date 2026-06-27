package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.domain.valueobject.config.SegmentTypeConfig;
import com.meritdata.mdm.codecenter.domain.valueobject.config.ReferenceSeqConfig;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.infrastructure.sequence.SequenceGenerator;
import com.meritdata.mdm.codecenter.application.service.WaterMarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 引用流水号段处理器.
 *
 * V0.5: 同样使用 claimNext 原子化水位, 避免高并发 race.
 */
@Component
@RequiredArgsConstructor
public class ReferenceSeqSegmentProcessor implements SegmentProcessor {

    private final SequenceGenerator sequenceGenerator;
    private final WaterMarkService waterMarkService;

    @Override
    public SegmentType supportedType() {
        return SegmentType.REFERENCE_SEQ;
    }

    @Override
    public SegmentResult process(CodeSegment segment, SegmentContext context) {
        ReferenceSeqConfig cfg = (ReferenceSeqConfig) SegmentTypeConfig.parse(SegmentType.REFERENCE_SEQ, segment.getConfigJson());

        if (cfg.refField() == null || cfg.refField().isEmpty()) {
            throw new IllegalArgumentException("REFERENCE_SEQ segment missing refField: " + segment.getSegmentCode());
        }
        String refValue = context.getDataString(cfg.refField());
        if (refValue == null || refValue.isEmpty()) {
            throw new IllegalArgumentException("REFERENCE_SEQ refField value is null: " + cfg.refField());
        }

        String bizTag = (cfg.bizTagPrefix() == null ? "MD:REF_SEQ" : cfg.bizTagPrefix())
                + ":" + refValue;
        sequenceGenerator.ensureBizTypeInitialized(bizTag, cfg.effectiveStep());

        Long seqNum = sequenceGenerator.nextSequence(bizTag, cfg.effectiveStep());

        if (!context.isSkipWaterMark()) {
            long actualSeq = waterMarkService.claimNext(bizTag, seqNum);
            if (actualSeq != seqNum) {
                seqNum = actualSeq;
            }
        }

        // Write back to context so callers can read the sequence number.
        context.setSequenceNum(seqNum);

        String padded = IdUtil.padSequence(seqNum, cfg.effectiveLength());

        return SegmentResult.builder()
                .segmentType(SegmentType.REFERENCE_SEQ)
                .segmentCode(segment.getSegmentCode())
                .segmentValue(padded)
                .sequenceBizTag(bizTag)
                .sequenceNum(seqNum)
                .detail("bizTag=" + bizTag + ",refField=" + cfg.refField()
                        + ",refValue=" + refValue + ",seqNum=" + seqNum)
                .build();
    }
}
