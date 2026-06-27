package com.meritdata.mdm.codecenter.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelAttributeRequest {
    private String id;
    private String modelId;
    private String cnName;
    private String enName;
    private String dataType;
    private Integer dataLength;
    private Integer decimalLength;
    private Boolean isRequired;
    private Boolean isUnique;
    private Boolean isCodeField;
    private String defaultValue;
    private String dictType;
    private Integer sortOrder;
    private String comment;
    private String tenantId;
}
