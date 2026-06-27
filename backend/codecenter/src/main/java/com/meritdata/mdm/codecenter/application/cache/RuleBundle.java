package com.meritdata.mdm.codecenter.application.cache;

import com.meritdata.mdm.codecenter.domain.entity.CodeRuleSegment;
import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.RuleMode;
import com.meritdata.mdm.codecenter.domain.enums.RuleStatus;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 规则只读快照 (V0.9 性能优化).
 *
 * 解决单次生成路径上 3 次 DB 往返 (rule/ruleSegments/segments) 的开销.
 * - CodeRule / CodeRuleSegment / CodeSegment 都是 JPA entity, 在事务结束后访问
 *   lazy 字段会抛 LazyInitializationException. 缓存 entity 也可能因 detached
 *   状态出现脏读/版本错乱. 这里只拷业务用到的标量字段, 缓存的是不可变快照.
 * - 缓存 key = (modelId, fieldId, version), 同一 (modelId, fieldId) 出现
 *   新版本时, 自动覆盖, 旧 entry 自然淘汰.
 * - TTL 由 RuleCacheService 控制 (默认 60s, publish/update 显式失效).
 */
public record RuleBundle(
        String ruleId,
        String modelId,
        String fieldId,
        Integer version,
        RuleStatus status,
        RuleMode ruleMode,
        String dslTemplate,
        String groovyScript,
        Integer recycleLockHours,
        String recycleStrategy,
        List<RuleSegmentRef> ruleSegments,
        Map<String, SegmentDef> segmentMap
) {
    /** 规则-码段关联快照. */
    public record RuleSegmentRef(String segmentId, int sortOrder) {}

    /** 码段定义快照. */
    public record SegmentDef(
            String id,
            String code,
            String name,
            SegmentType type,
            String configJson,
            Boolean isArchived
    ) {}

    /**
     * 从 entity 构造快照. 必须在事务内调用, 但返回的对象与持久化上下文解耦,
     * 可在事务外安全访问.
     */
    public static RuleBundle fromEntities(
            com.meritdata.mdm.codecenter.domain.entity.CodeRule rule,
            List<CodeRuleSegment> ruleSegments,
            List<CodeSegment> segments
    ) {
        Map<String, SegmentDef> map = segments.stream().collect(Collectors.toMap(
                CodeSegment::getId,
                s -> new SegmentDef(
                        s.getId(),
                        s.getSegmentCode(),
                        s.getSegmentName(),
                        s.getSegmentType(),
                        s.getConfigJson(),
                        s.getIsArchived()
                )
        ));
        List<RuleSegmentRef> refs = ruleSegments.stream()
                .map(rs -> new RuleSegmentRef(rs.getSegmentId(),
                        rs.getSortOrder() == null ? 0 : rs.getSortOrder()))
                .collect(Collectors.toList());
        return new RuleBundle(
                rule.getId(),
                rule.getModelId(),
                rule.getEncodeFieldId(),
                rule.getVersion(),
                rule.getStatus(),
                rule.getRuleMode(),
                rule.getDslTemplate(),
                rule.getGroovyScript(),
                rule.getRecycleLockHours(),
                rule.getRecycleStrategy(),
                refs,
                map
        );
    }
}
