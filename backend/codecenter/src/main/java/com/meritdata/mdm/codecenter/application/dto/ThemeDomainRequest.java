package com.meritdata.mdm.codecenter.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeDomainRequest {
    private String id;
    private String parentId;
    private String domainCode;
    private String domainName;
    private Integer sortOrder;
    private String remark;
    private String tenantId;
}
