package com.meritdata.mdm.codecenter.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 规则-码段关联表
 *
 * 一条 CodeRule -> 多个 CodeRuleSegment -> 多个 CodeSegment
 * 用于：顺序、重置条件、租户隔离
 */
@Entity
@Table(name = "md_code_rule_segment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rule_order", columnNames = {"ruleId", "sortOrder"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeRuleSegment {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "rule_id", nullable = false, length = 32)
    private String ruleId;

    @Column(name = "segment_id", nullable = false, length = 32)
    private String segmentId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** 重置条件：仅 SEQUENCE / REFERENCE_SEQ 可能存在 */
    @Column(name = "reset_condition", length = 64)
    private String resetCondition;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
