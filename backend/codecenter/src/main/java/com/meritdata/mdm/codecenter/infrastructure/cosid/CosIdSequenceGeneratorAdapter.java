package com.meritdata.mdm.codecenter.infrastructure.cosid;

import com.meritdata.mdm.codecenter.infrastructure.sequence.SequenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CosId 到 SequenceGenerator 接口的适配器
 */
@Primary
@Component
@RequiredArgsConstructor
public class CosIdSequenceGeneratorAdapter implements SequenceGenerator {

    private final CosIdGenerator cosIdGenerator;

    @Override
    public Long nextSequence(String bizType) {
        return cosIdGenerator.nextSequence(bizType);
    }

    @Override
    public Long nextSequence(String bizType, int step) {
        return cosIdGenerator.nextSequence(bizType);
    }

    @Override
    public List<Long> nextSequenceBatch(String bizType, int count) {
        return cosIdGenerator.nextSequenceBatch(bizType, count);
    }

    @Override
    public void ensureBizTypeInitialized(String bizType) {
        cosIdGenerator.ensureBizTypeInitialized(bizType);
    }

    @Override
    public void ensureBizTypeInitialized(String bizType, int step) {
        cosIdGenerator.ensureBizTypeInitialized(bizType);
    }
}
