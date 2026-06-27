package com.meritdata.mdm.codecenter.domain.valueobject.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;

/**
 * 日期码配置
 *
 * 示例: { "format": "yyyyMMdd", "dateSource": "SYSTEM" }
 *        { "format": "yyyy", "dateSource": "ATTRIBUTE", "sourceField": "createYear" }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DateConfig(
        /** 日期格式（Java DateTimeFormatter 模式） */
        String format,
        /** 日期来源: SYSTEM (默认) / ATTRIBUTE (取属性值) */
        String dateSource,
        /** dateSource=ATTRIBUTE 时，指定源字段名 */
        String sourceField,
        /** 时区: 默认为系统时区 */
        String timezone
) implements SegmentTypeConfig {
    public SegmentType segmentType() { return SegmentType.DATE; }
    public String effectiveDateSource() {
        return dateSource == null || dateSource.isEmpty() ? "SYSTEM" : dateSource;
    }
}
