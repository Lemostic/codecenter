package com.meritdata.mdm.codecenter.infrastructure.segment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class SegmentContext {

    private String modelId;
    private String fieldId;

    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    private String tenantId;
    private String baseBizTag;
    private Long sequenceNum;

    @Builder.Default
    private LocalDateTime now = LocalDateTime.now();

    @Builder.Default
    private Map<String, String> refFieldValues = new HashMap<>();

    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

    /**
     * True when the sequence is being reused from a recycled allocation.
     * The SEQUENCE processor should skip the water-mark check in this case,
     * because the sequence number came from a previously-persisted allocation.
     */
    @Builder.Default
    private boolean skipWaterMark = false;

    public String getDataString(String key) {
        Object v = data == null ? null : data.get(key);
        return v == null ? null : v.toString();
    }

    public Object getDataObject(String key) {
        return data == null ? null : data.get(key);
    }
}
