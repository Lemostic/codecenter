package com.meritdata.mdm.codecenter.domain.enums;

/**
 * 流水策略
 *
 *   NUMBER - 纯数字流水（默认），单调递增不重置
 *   CYCLE  - 循环流水，配合 RESET 按日/月/维度重置
 */
public enum SerialStrategy {
    NUMBER,
    CYCLE
}
