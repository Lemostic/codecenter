package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.domain.valueobject.config.ReferenceConfig;
import com.meritdata.mdm.codecenter.domain.valueobject.config.SegmentTypeConfig;
import org.springframework.stereotype.Component;

/**
 * 引用码段处理器
 *
 * 通过 associatedAttribute 字段（refField）从数据上下文中取值
 * 支持 LEFT / RIGHT / MIDDLE 三种截取方向
 * 真实跨模型引用通过 预加载数据上下文 实现
 */
@Component
public class ReferenceSegmentProcessor implements SegmentProcessor {

    @Override
    public SegmentType supportedType() {
        return SegmentType.REFERENCE;
    }

    @Override
    public SegmentResult process(CodeSegment segment, SegmentContext context) {
        ReferenceConfig cfg = (ReferenceConfig) SegmentTypeConfig.parse(SegmentType.REFERENCE, segment.getConfigJson());

        String refValue = null;
        if (cfg.associatedAttribute() != null) {
            refValue = context.getDataString(cfg.associatedAttribute());
        }
        if (refValue == null) {
            // 回退到 refFieldId
            refValue = context.getDataString(cfg.refFieldId());
        }
        if (refValue == null) {
            throw new IllegalArgumentException("REFERENCE segment: refValue not found, field="
                    + cfg.associatedAttribute() + " or " + cfg.refFieldId());
        }

        String sliced = slice(refValue, cfg.effectiveDirection(), cfg.effectiveStep(), cfg.startPosition());

        return SegmentResult.builder()
                .segmentType(SegmentType.REFERENCE)
                .segmentCode(segment.getSegmentCode())
                .segmentValue(sliced)
                .detail("direction=" + cfg.effectiveDirection() + ",step=" + cfg.effectiveStep()
                        + ",source=" + refValue)
                .build();
    }

    private String slice(String value, String direction, int step, Integer startPosition) {
        if (value == null) return "";
        if (step <= 0 || step >= value.length()) return value;
        return switch (direction) {
            case "RIGHT" -> value.substring(value.length() - step);
            case "MIDDLE" -> {
                int start = (startPosition == null || startPosition < 0) ? 0 : Math.min(startPosition, value.length() - step);
                yield value.substring(start, Math.min(start + step, value.length()));
            }
            default -> value.substring(0, step);
        };
    }
}
