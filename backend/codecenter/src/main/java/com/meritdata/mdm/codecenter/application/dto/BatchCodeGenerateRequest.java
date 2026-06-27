package com.meritdata.mdm.codecenter.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCodeGenerateRequest {
    @NotBlank
    private String modelId;
    @NotBlank
    private String fieldId;
    private String dataId;
    private Map<String, Object> data;
    private String tenantId;
    @Min(1)
    private Integer count;
}
