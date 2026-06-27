package com.meritdata.mdm.codecenter.domain.entity;

import com.meritdata.mdm.codecenter.domain.enums.ModelStatus;
import com.meritdata.mdm.codecenter.domain.enums.ModelType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 主数据模型主表
 */
@Entity
@Table(name = "md_model", indexes = {
        @Index(name = "idx_theme", columnList = "themeId"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_model_tenant", columnList = "tenantId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_model_code", columnNames = {"tenantId", "modelCode"}),
        @UniqueConstraint(name = "uk_table_name", columnNames = {"tenantId", "tableName"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Model {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "model_code", nullable = false, length = 50)
    private String modelCode;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 20)
    private ModelType modelType;

    @Column(name = "theme_id", length = 32)
    private String themeId;

    @Column(length = 500)
    private String description;

    /** 密级：INTERNAL/CONFIDENTIAL/SECRET/TOPSECRET */
    @Column(name = "security_level", length = 16)
    private String securityLevel;

    /** 当前版本号 */
    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ModelStatus status;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @Column(name = "create_by", length = 64)
    private String createBy;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_by", length = 64)
    private String updateBy;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}

