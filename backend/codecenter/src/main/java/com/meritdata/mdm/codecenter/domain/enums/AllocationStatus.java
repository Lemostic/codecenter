package com.meritdata.mdm.codecenter.domain.enums;

/**
 * 编码分配状态（V0.3 4 态）
 *
 *   PENDING    预占中 - 已生成但未确认
 *   USED       已使用 - 业务已确认使用，terminal
 *   CANCELLED  已取消 - 用户取消或校验失败，可能进入回收池
 *   RECYCLED   已回收 - 业务数据删除后回收
 */
public enum AllocationStatus {
    PENDING,
    USED,
    CANCELLED,
    RECYCLED;

    public boolean isTerminal() {
        return this == USED;
    }

    public boolean isRecyclable() {
        return this == CANCELLED || this == RECYCLED;
    }
}
