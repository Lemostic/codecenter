package com.meritdata.mdm.codecenter.domain.valueobject.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;

import java.util.Map;

/**
 * 特征码配置
 *
 * 示例: {
 *   "sourceField": "category",
 *   "mappingTable": { "原材料": "MC", "成品": "FP" },
 *   "defaultValue": "OT"
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EigenvalueConfig(
        /** 源属性字段名 */
        String sourceField,
        /** 映射表: 属性值 -> 编码 */
        Map<String, String> mappingTable,
        /** 未命中时的默认值 */
        String defaultValue
) implements SegmentTypeConfig {
    public SegmentType segmentType() { return SegmentType.EIGENVALUE; }
    public String lookup(String sourceValue) {
        if (sourceValue == null) return defaultValue;
        String mapped = mappingTable == null ? null : mappingTable.get(sourceValue);
        return mapped != null ? mapped : defaultValue;
    }
}
