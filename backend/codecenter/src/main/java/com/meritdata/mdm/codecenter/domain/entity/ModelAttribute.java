package com.meritdata.mdm.codecenter.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 模型属性（核心元数据）
 *
 * 每条记录 = 模型的一个字段定义
 */
@Entity
@Table(name = "md_model_attribute", indexes = {
        @Index(name = "idx_model_sort", columnList = "modelId,sortOrder"),
        @Index(name = "idx_code_field", columnList = "modelId,isCodeField")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_model_enname", columnNames = {"modelId", "enName"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelAttribute {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "model_id", nullable = false, length = 32)
    private String modelId;

    @Column(name = "cn_name", nullable = false, length = 100)
    private String cnName;

    @Column(name = "en_name", nullable = false, length = 100)
    private String enName;

    /** STRING/INTEGER/LONG/DOUBLE/DATE/DATETIME/BOOLEAN */
    @Column(name = "data_type", nullable = false, length = 32)
    private String dataType;

    @Column(name = "data_length")
    private Integer dataLength;

    @Column(name = "decimal_length")
    private Integer decimalLength;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;

    @Column(name = "is_unique", nullable = false)
    private Boolean isUnique;

    /** 是否为编码字段 - 模型中标记为编码的属性才能配置编码规则 */
    @Column(name = "is_code_field", nullable = false)
    private Boolean isCodeField;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "dict_type", length = 64)
    private String dictType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(length = 500)
    private String comment;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
