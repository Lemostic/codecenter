package com.meritdata.mdm.codecenter.application.dto;

import com.meritdata.mdm.codecenter.domain.enums.GenerateTrigger;
import com.meritdata.mdm.codecenter.domain.enums.RuleMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeRuleRequest {
    private String id;
    private String modelId;
    private String encodeFieldId;
    private String ruleName;
    private String ruleCode;
    private String ruleDesc;
    private RuleMode ruleMode;
    private GenerateTrigger triggerType;
    private String dslTemplate;
    private String groovyScript;
    private Integer recycleLockHours;
    private String recycleStrategy;
    private String tenantId;
    /** 码段引用：[{segmentId, sortOrder, resetCondition}] */
    private List<RuleSegmentRef> segments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleSegmentRef {
        private String segmentId;
        private Integer sortOrder;
        private String resetCondition;
    }
}
