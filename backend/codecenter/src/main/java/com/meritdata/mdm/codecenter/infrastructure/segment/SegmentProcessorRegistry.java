package com.meritdata.mdm.codecenter.infrastructure.segment;

import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 码段处理器注册中心
 *
 * 自动收集所有 SegmentProcessor 实现，按 supportedType 注册到 EnumMap
 * 编排放行时通过 get(segmentType) 取出对应处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SegmentProcessorRegistry {

    private final List<SegmentProcessor> processors;
    private final Map<SegmentType, SegmentProcessor> registry = new EnumMap<>(SegmentType.class);

    @PostConstruct
    public void init() {
        for (SegmentProcessor p : processors) {
            SegmentType t = p.supportedType();
            if (registry.put(t, p) != null) {
                log.warn("Multiple processors for {}: existing={}, new={}",
                        t, registry.get(t).getClass().getSimpleName(), p.getClass().getSimpleName());
            }
        }
        log.info("SegmentProcessorRegistry initialized: {} types = {}", registry.size(), registry.keySet());
    }

    public SegmentProcessor get(SegmentType type) {
        SegmentProcessor p = registry.get(type);
        if (p == null) {
            throw new IllegalStateException("No SegmentProcessor for type: " + type);
        }
        return p;
    }

    public boolean has(SegmentType type) {
        return registry.containsKey(type);
    }
}
