package com.meritdata.mdm.codecenter.domain.entity;

import com.meritdata.mdm.codecenter.domain.enums.AllocationStatus;
import com.meritdata.mdm.codecenter.domain.enums.WasteType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 编码分配表（V0.3 合并设计 - 预占+使用+取消+回收一体化）
 *
 *   状态机:
 *     PENDING    - 预占中（业务未确认）
 *     USED       - 已使用（terminal，永久保留审计）
 *     CANCELLED  - 已取消（可进入回收池，锁定期内不可复用）
 *     RECYCLED   - 已回收（业务数据删除后进入回收池）
 */
@Entity
@Table(name = "md_code_allocation", indexes = {
        @Index(name = "idx_rule_status", columnList = "ruleId,status"),
        @Index(name = "idx_recycle", columnList = "ruleId,status,recycleLockTime"),
        @Index(name = "idx_expire", columnList = "status,isExposed,allocateTime"),
        @Index(name = "idx_data", columnList = "dataId"),
        @Index(name = "idx_alloc_tenant", columnList = "tenantId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_code", columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeAllocation {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "rule_id", nullable = false, length = 32)
    private String ruleId;

    @Column(name = "rule_version_id", length = 32)
    private String ruleVersionId;

    @Column(nullable = false, length = 200)
    private String code;

    @Column(name = "sequence_num")
    private Long sequenceNum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private AllocationStatus status;

    @Column(name = "is_exposed", nullable = false)
    private Integer isExposed;

    @Column(name = "is_archived", nullable = false)
    private Integer isArchived;

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_type", length = 20)
    private WasteType wasteType;

    /** 码段拆解明细 JSON */
    @Lob
    @Column(name = "segment_values", columnDefinition = "TEXT")
    private String segmentValues;

    @Column(name = "data_id", length = 64)
    private String dataId;

    @Column(name = "allocate_time", nullable = false)
    private LocalDateTime allocateTime;

    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    @Column(name = "used_time")
    private LocalDateTime usedTime;

    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;

    @Column(name = "recycle_time")
    private LocalDateTime recycleTime;

    @Column(name = "recycle_lock_time")
    private LocalDateTime recycleLockTime;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @Version
    @Column(nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /* ============== 领域行为 ============== */

    public void confirm() {
        if (this.status != AllocationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING can be confirmed, current=" + this.status);
        }
        this.status = AllocationStatus.USED;
        this.confirmTime = LocalDateTime.now();
        this.usedTime = this.confirmTime;
    }

    public void cancel(WasteType wasteType) {
        if (this.status != AllocationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING can be cancelled, current=" + this.status);
        }
        this.status = AllocationStatus.CANCELLED;
        this.wasteType = wasteType;
        this.cancelTime = LocalDateTime.now();
    }

    public void recycle() {
        if (this.status != AllocationStatus.USED) {
            throw new IllegalStateException("Only USED can be recycled, current=" + this.status);
        }
        this.status = AllocationStatus.RECYCLED;
        this.recycleTime = LocalDateTime.now();
    }
}

