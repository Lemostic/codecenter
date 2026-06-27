package com.meritdata.mdm.codecenter.domain.entity;

import com.meritdata.mdm.codecenter.domain.enums.GenerateTrigger;
import com.meritdata.mdm.codecenter.domain.enums.RuleMode;
import com.meritdata.mdm.codecenter.domain.enums.RuleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 编码规则聚合根
 *
 * 状态机: EDIT -> EFFECT -> HISTORY/DISABLED
 */
@Entity
@Table(name = "md_code_rule", indexes = {
        @Index(name = "idx_model", columnList = "modelId"),
        @Index(name = "idx_rule_tenant", columnList = "tenantId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_model_field_version", columnNames = {"modelId", "encodeFieldId", "version"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeRule {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "model_id", nullable = false, length = 32)
    private String modelId;

    /** 编码字段（model_attribute.id） */
    @Column(name = "encode_field_id", nullable = false, length = 32)
    private String encodeFieldId;

    @Column(name = "rule_name", nullable = false, length = 64)
    private String ruleName;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "rule_desc", length = 200)
    private String ruleDesc;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_mode", nullable = false, length = 16)
    private RuleMode ruleMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 16)
    private GenerateTrigger triggerType;

    @Column(name = "dsl_template", length = 1000)
    private String dslTemplate;

    @Lob
    @Column(name = "groovy_script", columnDefinition = "TEXT")
    private String groovyScript;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RuleStatus status;

    @Column(name = "recycle_lock_hours", nullable = false)
    private Integer recycleLockHours;

    @Column(name = "recycle_strategy", length = 16)
    private String recycleStrategy;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @OneToMany(mappedBy = "ruleId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<CodeRuleSegment> ruleSegments = new ArrayList<>();

    /* ============== 领域行为 ============== */

    public boolean containsSequence() {
        return dslTemplate != null && dslTemplate.contains("{SEQUENCE}");
    }

    public void publish() {
        if (this.status != RuleStatus.EDIT) {
            throw new IllegalStateException("Only EDIT rule can be published, current=" + this.status);
        }
        this.status = RuleStatus.EFFECT;
        this.publishedAt = LocalDateTime.now();
    }

    public void disable() {
        if (this.status != RuleStatus.EFFECT && this.status != RuleStatus.EDIT) {
            throw new IllegalStateException("Only EFFECT/EDIT rule can be disabled, current=" + this.status);
        }
        this.status = RuleStatus.DISABLED;
        this.disabledAt = LocalDateTime.now();
    }

    public void enable() {
        if (this.status != RuleStatus.DISABLED) {
            throw new IllegalStateException("Only DISABLED rule can be enabled, current=" + this.status);
        }
        this.status = RuleStatus.EFFECT;
    }

    public void reviseToHistory() {
        if (this.status == RuleStatus.EFFECT) {
            this.status = RuleStatus.HISTORY;
        }
    }
}

