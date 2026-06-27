package com.meritdata.mdm.codecenter.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * 码段类型枚举（V0.3 - 6 类基础码段）
 *
 * 复杂场景（日期流水 / 动态流水 / 区间流水）通过原子码段组合实现：
 *   日期流水      = DATE + SEQUENCE + reset=daily
 *   动态流水      = REFERENCE_SEQ + reset
 *   区间流水      = EIGENVALUE + SEQUENCE + 区间配置
 */
public enum SegmentType {

    /** 固定码 — 常量字符串 */
    FIXED,

    /** 日期码 — 格式化日期（系统时间 / 属性值） */
    DATE,

    /** 流水码 — 纯数字单调递增 */
    SEQUENCE,

    /** 特征码 — 属性值通过映射表转换为编码 */
    EIGENVALUE,

    /** 引用码 — 引用其他模型属性值 */
    REFERENCE,

    /** 引用流水码 — 引用属性值变化时流水号重置 */
    REFERENCE_SEQ;

    @JsonValue
    public String code() {
        return name();
    }

    @JsonCreator
    public static SegmentType fromCode(String code) {
        if (code == null || code.isEmpty()) return null;
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown segment type: " + code + ", allowed=" + Arrays.toString(values())));
    }

    public boolean isSequenceType() {
        return this == SEQUENCE || this == REFERENCE_SEQ;
    }

    public boolean isReferenceType() {
        return this == REFERENCE || this == REFERENCE_SEQ;
    }

    public boolean isDateType() {
        return this == DATE;
    }
}
