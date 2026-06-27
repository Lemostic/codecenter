package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import lombok.Builder;
import lombok.Data;

/**
 * 码段处理结果
 */
@Data
@Builder
public class SegmentResult {
    /** 码段类型 */
    private SegmentType segmentType;
    /** 码段编码（业务标识） */
    private String segmentCode;
    /** 码段值（拼接进最终编码的字符串） */
    private String segmentValue;
    /** 流水号相关（仅 SEQUENCE / REFERENCE_SEQ 类型） */
    private String sequenceBizTag;
    private Long sequenceNum;
    /** 用于审计/调试的元数据 */
    private String detail;
}
