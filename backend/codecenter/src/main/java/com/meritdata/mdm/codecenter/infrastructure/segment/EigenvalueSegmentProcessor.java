package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.domain.valueobject.config.EigenvalueConfig;
import com.meritdata.mdm.codecenter.domain.valueobject.config.SegmentTypeConfig;
import org.springframework.stereotype.Component;

/**
 * 特征码段处理器
 *
 * 读取 sourceField 属性值，根据 mappingTable 映射为编码
 * 缓存: 通过 Caffeine 缓存配置（由配置中心维护时使用）
 */
@Component
public class EigenvalueSegmentProcessor implements SegmentProcessor {

    @Override
    public SegmentType supportedType() {
        return SegmentType.EIGENVALUE;
    }

    @Override
    public SegmentResult process(CodeSegment segment, SegmentContext context) {
        EigenvalueConfig cfg = (EigenvalueConfig) SegmentTypeConfig.parse(SegmentType.EIGENVALUE, segment.getConfigJson());
        if (cfg.sourceField() == null) {
            throw new IllegalArgumentException("EIGENVALUE segment missing sourceField: " + segment.getSegmentCode());
        }
        String sourceValue = context.getDataString(cfg.sourceField());
        String mapped = cfg.lookup(sourceValue);

        return SegmentResult.builder()
                .segmentType(SegmentType.EIGENVALUE)
                .segmentCode(segment.getSegmentCode())
                .segmentValue(mapped == null ? "" : mapped)
                .detail("sourceField=" + cfg.sourceField() + ",sourceValue=" + sourceValue
                        + ",mapped=" + mapped)
                .build();
    }
}
