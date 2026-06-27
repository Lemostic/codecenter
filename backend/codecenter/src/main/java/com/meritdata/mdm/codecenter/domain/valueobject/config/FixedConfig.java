package com.meritdata.mdm.codecenter.domain.valueobject.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;

/**
 * 固定码配置
 * 示例: { "value": "WL", "prefix": "", "suffix": "" }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FixedConfig(
        /** 固定值（码段内容） */
        String value,
        /** 前缀（拼接前缀） */
        String prefix,
        /** 后缀（拼接后缀） */
        String suffix
) implements SegmentTypeConfig {
    public SegmentType segmentType() { return SegmentType.FIXED; }
}
