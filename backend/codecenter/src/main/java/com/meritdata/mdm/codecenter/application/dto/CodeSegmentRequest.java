package com.meritdata.mdm.codecenter.application.dto;

import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSegmentRequest {
    private String id;
    private String segmentCode;
    private String segmentName;
    private SegmentType segmentType;
    private String configJson;
    private String description;
    private String tenantId;
}
