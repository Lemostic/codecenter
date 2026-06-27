package com.meritdata.mdm.codecenter.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 主题域 - 模型分类树
 */
@Entity
@Table(name = "md_theme_domain", indexes = {
        @Index(name = "idx_parent", columnList = "parentId"),
        @Index(name = "idx_theme_tenant", columnList = "tenantId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_domain_code", columnNames = {"tenantId", "domainCode"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemeDomain {

    @Id
    @Column(length = 32)
    private String id;

    /** 父节点 ID（顶层主题域为 null） */
    @Column(name = "parent_id", length = 32)
    private String parentId;

    /** 主题域编码 */
    @Column(name = "domain_code", nullable = false, length = 50)
    private String domainCode;

    /** 主题域名称 */
    @Column(name = "domain_name", nullable = false, length = 50)
    private String domainName;

    /** 排序号 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** 备注 */
    @Column(length = 500)
    private String remark;

    /** 租户 */
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

