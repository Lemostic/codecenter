package com.meritdata.mdm.codecenter.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 主数据模型状态机
 *
 *   EDIT       编辑中
 *   AUDITING   审核中（可选）
 *   EFFECT     生效
 *   HISTORY    历史
 *   DISABLED   停用
 */
public enum ModelStatus {
    EDIT,
    AUDITING,
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
    public static ModelStatus fromString(String value) {
        if (value == null) return null;
        return ModelStatus.valueOf(value.trim().toUpperCase());
    }
}
