package com.meritdata.mdm.codecenter.domain.enums;

/**
 * 编码废码分类
 */
public enum WasteType {
    /** 超时 - PENDING 超过 30min 未确认 */
    TIMEOUT,
    /** 取消 - 用户主动取消/关闭页面 */
    CANCEL,
    /** 异常 - 码段处理异常 */
    FAIL,
    /** 删除 - 业务数据删除 */
    DELETE
}
