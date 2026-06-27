package com.meritdata.mdm.codecenter.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeGenerateRequest {
    /** 模型 ID */
    private String modelId;
    /** 编码字段 ID */
    private String fieldId;
    /** 业务数据 ID（可选） */
    private String dataId;
    /** 业务数据：属性名 -> 值 */
    private Map<String, Object> data;
    /** 租户 ID */
    private String tenantId;
}
