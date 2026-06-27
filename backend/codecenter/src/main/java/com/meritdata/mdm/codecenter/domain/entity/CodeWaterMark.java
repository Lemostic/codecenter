package com.meritdata.mdm.codecenter.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 号段水位表（V0.3 关键创新）
 *
 * 解决 导入→冲突→重生成→冲突 死循环问题
 * 每个 bizTag 维护一个水位线，记录历史最大已分配流水号
 * 新生成时跳过已用水位以下的号码
 *
 * V0.4: 不使用 @Version 乐观锁 - 改用悲观锁 (SELECT FOR UPDATE) 串行化并发访问，
 *       UPDATE 用原子条件 (current_water < :newWater) 保证单次更新无冲突。
 */
@Entity
@Table(name = "md_code_water_mark", uniqueConstraints = {
        @UniqueConstraint(name = "uk_biz_tag", columnNames = "bizTag")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeWaterMark {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "biz_tag", nullable = false, length = 128)
    private String bizTag;

    @Column(name = "rule_id", nullable = false, length = 32)
    private String ruleId;

    @Column(name = "current_water", nullable = false)
    private Long currentWater;

    @Column(name = "last_allocate_time")
    private LocalDateTime lastAllocateTime;

    @Column(name = "last_calibrate_time")
    private LocalDateTime lastCalibrateTime;

    @Column(name = "calibrate_source", length = 20)
    private String calibrateSource;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
