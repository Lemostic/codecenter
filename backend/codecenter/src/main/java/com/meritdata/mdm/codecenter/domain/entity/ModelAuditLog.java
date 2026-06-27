package com.meritdata.mdm.codecenter.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 操作审计日志（仅追加）
 *
 * 记录模型/规则/码段的关键操作，支持 i18n 错误码追溯
 */
@Entity
@Table(name = "md_model_audit_log", indexes = {
        @Index(name = "idx_target", columnList = "targetId,targetType"),
        @Index(name = "idx_operator", columnList = "operatorId"),
        @Index(name = "idx_time", columnList = "operatedAt"),
        @Index(name = "idx_log_tenant", columnList = "tenantId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelAuditLog {

    @Id
    @Column(name = "log_id", length = 32)
    private String logId;

    @Column(name = "operator_id", nullable = false, length = 32)
    private String operatorId;

    @Column(name = "operator_name", length = 64)
    private String operatorName;

    @Column(name = "operation_type", nullable = false, length = 32)
    private String operationType;

    @Column(name = "target_id", nullable = false, length = 32)
    private String targetId;

    @Column(name = "target_type", nullable = false, length = 16)
    private String targetType;

    @Column(name = "before_state", length = 32)
    private String beforeState;

    @Column(name = "after_state", length = 32)
    private String afterState;

    @Lob
    @Column(name = "diff_snapshot", columnDefinition = "TEXT")
    private String diffSnapshot;

    @Column(name = "operator_ip", length = 64)
    private String operatorIp;

    @Column(name = "operated_at", nullable = false)
    private LocalDateTime operatedAt;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}

