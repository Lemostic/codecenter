package com.meritdata.mdm.codecenter.infrastructure.sequence;

import java.util.List;

/**
 * 序列号生成器接口
 */
public interface SequenceGenerator {

    Long nextSequence(String bizType);

    Long nextSequence(String bizType, int step);

    List<Long> nextSequenceBatch(String bizType, int count);

    void ensureBizTypeInitialized(String bizType);

    void ensureBizTypeInitialized(String bizType, int step);
}
