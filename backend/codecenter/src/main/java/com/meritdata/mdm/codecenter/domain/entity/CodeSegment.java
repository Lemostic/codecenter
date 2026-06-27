package com.meritdata.mdm.codecenter.domain.entity;

import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 码段主数据（V0.3 - 6 类基础码段）
 */
@Entity
@Table(name = "md_code_segment", indexes = {
        @Index(name = "idx_type", columnList = "segmentType"),
        @Index(name = "idx_seg_tenant", columnList = "tenantId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_segment_code", columnNames = {"tenantId", "segmentCode"}),
        @UniqueConstraint(name = "uk_segment_name", columnNames = {"tenantId", "segmentName"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSegment {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "segment_code", nullable = false, length = 64)
    private String segmentCode;

    @Column(name = "segment_name", nullable = false, length = 100)
    private String segmentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment_type", nullable = false, length = 32)
    private SegmentType segmentType;

    /** 详细配置 JSON（不同类型不同结构） */
    @Lob
    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Column(length = 200)
    private String description;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

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
}

