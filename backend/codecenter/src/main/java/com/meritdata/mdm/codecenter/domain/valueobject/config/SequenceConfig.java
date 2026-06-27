package com.meritdata.mdm.codecenter.domain.valueobject.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;

/**
 * 流水码配置
 *
 * 示例: { "length": 6, "bizTag": "MD:MODEL_MATERIAL:FIELD_CODE", "startValue": 1, "step": 1 }
 *        { "length": 4, "bizTag": "MD:MODEL:ORDER_NO", "reset": "DAILY" }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SequenceConfig(
        /** 流水号总长度（不足前面补 0） */
        Integer length,
        /** 业务号段标识（CosId bizTag） */
        String bizTag,
        /** 起始值（默认 1） */
        Long startValue,
        /** 步长（默认 1） */
        Integer step,
        /** 重置策略: NONE / DAILY / MONTHLY */
        String reset
) implements SegmentTypeConfig {
    public SegmentType segmentType() { return SegmentType.SEQUENCE; }
    public int effectiveLength() { return length == null || length <= 0 ? 6 : length; }
    public int effectiveStep() { return step == null || step <= 0 ? 1 : step; }
    public long effectiveStartValue() { return startValue == null || startValue < 0 ? 0L : startValue; }
    public String effectiveReset() { return reset == null || reset.isEmpty() ? "NONE" : reset; }
}
