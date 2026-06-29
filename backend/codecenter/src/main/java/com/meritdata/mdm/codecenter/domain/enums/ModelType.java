package com.meritdata.mdm.codecenter.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 主数据模型类型
 *
 *   NORMAL    普通模型 - 单一独立业务实体，扁平化结构
 *   COMPOSITE 复合模型 - 主模型 + 子模型
 *   CLASSIFY  分类模型 - 分类树管理
 */
public enum ModelType {
    NORMAL,
    COMPOSITE,
    CLASSIFY;

    /**
     * 兼容前端大小写：normal/composite/classification/classify
     */
    @JsonCreator
    public static ModelType fromString(String value) {
        if (value == null) return null;
        String v = value.trim().toUpperCase();
        // classification → CLASSIFY
        if ("CLASSIFICATION".equals(v)) return CLASSIFY;
        return ModelType.valueOf(v);
    }
}