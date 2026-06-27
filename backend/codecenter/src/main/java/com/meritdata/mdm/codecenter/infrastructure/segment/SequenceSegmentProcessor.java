package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.domain.valueobject.config.SegmentTypeConfig;
import com.meritdata.mdm.codecenter.domain.valueobject.config.SequenceConfig;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.infrastructure.sequence.SequenceGenerator;
import com.meritdata.mdm.codecenter.application.service.WaterMarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 号段处理器.
 *
 * V0.5 关键修复: 合并"是否低于水位 + 分配"两步为单次原子 claimNext.
 *   - 之前分两条路径 (isBelowWater + recordAndReturn / recordAllocation)
 *     在 50+ 线程并发下存在 race: 线程 A 看到水位低, 线程 B 看到水位高,
 *     两条路径交错执行时会导致两个线程拿到相同 seq.
 *   - 现在统一走 claimNext: SELECT ... FOR UPDATE + 决策 + UPDATE,
 *     同一时刻只有一个线程能修改某个 bizTag 的水位行, 实际返回的 seq
 *     必然 > 所有已分配 seq.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SequenceSegmentProcessor implements SegmentProcessor {

    private final SequenceGenerator sequenceGenerator;
    private final WaterMarkService waterMarkService;

    @Override
    public SegmentType supportedType() {
        return SegmentType.SEQUENCE;
    }

    @Override
    public SegmentResult process(CodeSegment segment, SegmentContext context) {
        SequenceConfig cfg = (SequenceConfig) SegmentTypeConfig.parse(SegmentType.SEQUENCE, segment.getConfigJson());

        String bizTag = buildBizTag(cfg, context);
        sequenceGenerator.ensureBizTypeInitialized(bizTag, cfg.effectiveStep());

        Long seqNum = context.getSequenceNum();
        boolean fromContext = seqNum != null;
        if (seqNum == null) {
            seqNum = sequenceGenerator.nextSequence(bizTag, cfg.effectiveStep());
        }
        if (log.isDebugEnabled()) {
            log.debug("SeqProc bizTag={} cosidSeq={} ctxId={} fromContext={}",
                    bizTag, seqNum, System.identityHashCode(context), fromContext);
        }

        // V0.5: 统一走 claimNext, 原子化"读-改-写"水位, 无论 CosId 给什么 seq
        // 都能保证最终 seq > 所有已分配的 seq, 彻底消除高并发下的 race.
        // 复用回收池 (fromContext && skipWaterMark) 时跳过水位校验.
        if (!context.isSkipWaterMark()) {
            long actualSeq = waterMarkService.claimNext(bizTag, seqNum);
            if (log.isDebugEnabled() && actualSeq != seqNum) {
                log.debug("SeqProc water-rewrote bizTag={} from={} to={} ctxId={}",
                        bizTag, seqNum, actualSeq, System.identityHashCode(context));
            }
            seqNum = actualSeq;
        }

        // Write back to context so callers (e.g. allocation persistence) can read it.
        context.setSequenceNum(seqNum);

        String padded = IdUtil.padSequence(seqNum, cfg.effectiveLength());

        return SegmentResult.builder()
                .segmentType(SegmentType.SEQUENCE)
                .segmentCode(segment.getSegmentCode())
                .segmentValue(padded)
                .sequenceBizTag(bizTag)
                .sequenceNum(seqNum)
                .detail("bizTag=" + bizTag + ",length=" + cfg.effectiveLength() + ",seqNum=" + seqNum)
                .build();
    }

    private String buildBizTag(SequenceConfig cfg, SegmentContext context) {
        if (cfg.bizTag() != null && !cfg.bizTag().isEmpty()) {
            return applyReset(cfg.bizTag(), cfg.effectiveReset(), context.getNow());
        }
        String base = context.getBaseBizTag() == null ? "MD" : context.getBaseBizTag();
        return applyReset(base, cfg.effectiveReset(), context.getNow());
    }

    private String applyReset(String base, String reset, LocalDateTime now) {
        return switch (reset) {
            case "DAILY" -> base + ":D" + now.toLocalDate();
            case "MONTHLY" -> base + ":M" + now.getYear() + String.format("%02d", now.getMonthValue());
            default -> base;
        };
    }
}
