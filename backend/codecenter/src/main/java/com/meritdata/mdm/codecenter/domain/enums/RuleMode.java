package com.meritdata.mdm.codecenter.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 编码规则定义方式
 */
public enum RuleMode {
    /** 码段组合 - 可视化拼装 */
    DSL,
    /** 脚本自定义 - Groovy 脚本 */
    GROOVY;

    @JsonCreator
    public static RuleMode fromString(String value) {
        if (value == null) return null;
        return RuleMode.valueOf(value.trim().toUpperCase());
    }
}
