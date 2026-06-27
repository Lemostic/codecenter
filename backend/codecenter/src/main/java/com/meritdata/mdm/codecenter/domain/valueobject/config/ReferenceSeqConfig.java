package com.meritdata.mdm.codecenter.domain.valueobject.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;

/**
 * 引用流水码配置
 *
 * 引用属性值变化时，流水号重置（按引用属性值维度独立计数）
 * 示例: { "refField": "contractCode", "length": 4, "bizTagPrefix": "MD:CONTRACT" }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReferenceSeqConfig(
        /** 触发重置的引用属性字段名 */
        String refField,
        /** 流水号长度 */
        Integer length,
        /** bizTag 前缀（不同 refField 值会拼到 bizTag 后） */
        String bizTagPrefix,
        /** 起始值 */
        Long startValue,
        /** 步长 */
        Integer step
) implements SegmentTypeConfig {
    public SegmentType segmentType() { return SegmentType.REFERENCE_SEQ; }
    public int effectiveLength() { return length == null || length <= 0 ? 4 : length; }
    public int effectiveStep() { return step == null || step <= 0 ? 1 : step; }
    public long effectiveStartValue() { return startValue == null || startValue < 0 ? 0L : startValue; }
}
