package com.meritdata.mdm.codecenter.domain.valueobject.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;

import java.util.HashMap;
import java.util.Map;

/**
 * 码段配置值对象 - 顶层 sealed 接口
 *
 * V0.3 - 6 类基础码段配置
 * 复合场景通过原子操作组合实现（日期流水、动态流水、区间流水）
 */
public sealed interface SegmentTypeConfig
        permits FixedConfig, DateConfig, SequenceConfig, EigenvalueConfig,
        ReferenceConfig, ReferenceSeqConfig {

    SegmentType segmentType();

    /** 解析码段配置（按类型分发） */
    static SegmentTypeConfig parse(SegmentType type, String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            throw new IllegalArgumentException("configJson is required for " + type);
        }
        try {
            Map<String, Object> map = new HashMap<>();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(configJson);
            node.fields().forEachRemaining(e -> map.put(e.getKey(), stringValue(e.getValue())));

            return switch (type) {
                case FIXED -> mapper.readValue(configJson, FixedConfig.class);
                case DATE -> mapper.readValue(configJson, DateConfig.class);
                case SEQUENCE -> mapper.readValue(configJson, SequenceConfig.class);
                case EIGENVALUE -> mapper.readValue(configJson, EigenvalueConfig.class);
                case REFERENCE -> mapper.readValue(configJson, ReferenceConfig.class);
                case REFERENCE_SEQ -> mapper.readValue(configJson, ReferenceSeqConfig.class);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse segment config for " + type + ": " + e.getMessage(), e);
        }
    }

    private static Object stringValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble() || node.isFloat()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();
        return node.asText();
    }
}
