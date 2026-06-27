package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;

/**
 * 码段处理器 - 策略模式
 *
 * 6 类基础码段各对应一个实现，新增类型只需新增一个 @Component
 */
public interface SegmentProcessor {

    /** 当前处理器支持的码段类型 */
    SegmentType supportedType();

    /** 处理码段并返回结果 */
    SegmentResult process(CodeSegment segment, SegmentContext context);
}
