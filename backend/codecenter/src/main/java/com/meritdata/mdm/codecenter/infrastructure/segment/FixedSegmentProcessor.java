package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.domain.valueobject.config.FixedConfig;
import com.meritdata.mdm.codecenter.domain.valueobject.config.SegmentTypeConfig;
import org.springframework.stereotype.Component;

/**
 * 固定码段处理器
 *
 * 拼接规则: prefix + value + suffix
 * 示例: prefix="WL-", value="MAT", suffix="" -> "WL-MAT"
 */
@Component
public class FixedSegmentProcessor implements SegmentProcessor {

    @Override
    public SegmentType supportedType() {
        return SegmentType.FIXED;
    }

    @Override
    public SegmentResult process(CodeSegment segment, SegmentContext context) {
        FixedConfig cfg = (FixedConfig) SegmentTypeConfig.parse(SegmentType.FIXED, segment.getConfigJson());
        StringBuilder sb = new StringBuilder();
        if (cfg.prefix() != null) sb.append(cfg.prefix());
        sb.append(cfg.value() == null ? "" : cfg.value());
        if (cfg.suffix() != null) sb.append(cfg.suffix());

        return SegmentResult.builder()
                .segmentType(SegmentType.FIXED)
                .segmentCode(segment.getSegmentCode())
                .segmentValue(sb.toString())
                .detail("prefix=" + cfg.prefix() + ",value=" + cfg.value() + ",suffix=" + cfg.suffix())
                .build();
    }
}
