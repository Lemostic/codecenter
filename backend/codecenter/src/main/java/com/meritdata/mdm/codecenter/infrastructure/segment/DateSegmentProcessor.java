package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.domain.valueobject.config.DateConfig;
import com.meritdata.mdm.codecenter.domain.valueobject.config.SegmentTypeConfig;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateSegmentProcessor implements SegmentProcessor {

    @Override
    public SegmentType supportedType() {
        return SegmentType.DATE;
    }

    @Override
    public SegmentResult process(CodeSegment segment, SegmentContext context) {
        DateConfig cfg = (DateConfig) SegmentTypeConfig.parse(SegmentType.DATE, segment.getConfigJson());
        String format = cfg.format() == null || cfg.format().isEmpty() ? "yyyyMMdd" : cfg.format();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

        String value;
        if ("ATTRIBUTE".equalsIgnoreCase(cfg.effectiveDateSource()) && cfg.sourceField() != null) {
            Object source = context.getDataObject(cfg.sourceField());
            value = formatAttribute(source, formatter);
        } else {
            value = context.getNow().format(formatter);
        }

        return SegmentResult.builder()
                .segmentType(SegmentType.DATE)
                .segmentCode(segment.getSegmentCode())
                .segmentValue(value == null ? "" : value)
                .detail("format=" + format + ",source=" + cfg.effectiveDateSource())
                .build();
    }

    private String formatAttribute(Object source, DateTimeFormatter formatter) {
        if (source == null) return null;
        if (source instanceof LocalDate ld) return ld.format(formatter);
        if (source instanceof LocalDateTime ldt) return ldt.format(formatter);
        try {
            return LocalDate.parse(source.toString()).format(formatter);
        } catch (Exception ignored) {
            // try parsing as LocalDateTime
        }
        try {
            return LocalDateTime.parse(source.toString()).format(formatter);
        } catch (Exception ignored) {
            // try as-is
        }
        return source.toString();
    }
}
