package com.meritdata.mdm.codecenter.application.dto;

import com.meritdata.mdm.codecenter.domain.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRequest {
    private String id;
    private String modelCode;
    private String modelName;
    private String tableName;
    private ModelType modelType;
    private String themeId;
    private String description;
    private String securityLevel;
    private String tenantId;
    /** 同步创建属性 */
    private List<ModelAttributeRequest> attributes;
}
