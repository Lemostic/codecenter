package com.meritdata.mdm.codecenter.domain.enums;

/**
 * 编码生成时机
 */
public enum GenerateTrigger {
    /** 按钮生成 - 用户手动触发 */
    BUTTON,
    /** 保存时生成 - 保存/送审时触发 */
    SAVE,
    /** 生效时生成 - 数据生效时触发 */
    EFFECT
}
