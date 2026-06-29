package com.meritdata.mdm.codecenter.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 编码规则状态
 *
 *   EDIT     编辑中 - 默认状态，可编辑
 *   EFFECT   生效   - 业务可用，不可编辑
 *   HISTORY  历史   - 被新版本替代
 *   DISABLED 停用   - 主动停用
 */
public enum RuleStatus {
    EDIT,
    EFFECT,
    HISTORY,
    DISABLED;

    public boolean isEditable() {
        return this == EDIT;
    }

    public boolean isActive() {
        return this == EFFECT;
    }

    @JsonCreator
    public static RuleStatus fromString(String value) {
        if (value == null) return null;
        return RuleStatus.valueOf(value.trim().toUpperCase());
    }
}
