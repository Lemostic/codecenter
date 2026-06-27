package com.meritdata.mdm.codecenter.domain.valueobject.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;

/**
 * 引用码配置
 *
 * 示例: {
 *   "refModelId": "MD_M_CONTRACT",
 *   "refFieldId": "MD_CONTRACT_CODE",
 *   "cutDirection": "LEFT",       // LEFT | RIGHT | MIDDLE
 *   "cutStep": 3,
 *   "startPosition": 0
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReferenceConfig(
        /** 引用的模型 ID */
        String refModelId,
        /** 引用的属性 ID（编码字段） */
        String refFieldId,
        /** 截取方向: LEFT (从左截) / RIGHT (从右截) / MIDDLE (中间截) */
        String cutDirection,
        /** 截取步长 */
        Integer cutStep,
        /** 起始位置（MIDDLE 模式有效） */
        Integer startPosition,
        /** 关联属性名（数据传递时使用） */
        String associatedAttribute
) implements SegmentTypeConfig {
    public SegmentType segmentType() { return SegmentType.REFERENCE; }
    public String effectiveDirection() {
        return cutDirection == null || cutDirection.isEmpty() ? "LEFT" : cutDirection.toUpperCase();
    }
    public int effectiveStep() { return cutStep == null || cutStep <= 0 ? 0 : cutStep; }
}
