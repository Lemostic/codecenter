package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.CodeAllocation;
import com.meritdata.mdm.codecenter.domain.enums.AllocationStatus;
import com.meritdata.mdm.codecenter.domain.repository.CodeAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回收策略服务
 *
 * 单源原则 (V0.3 会议结论):
 *   一次批量请求 -> 决策一个来源 (回收池 OR 号段) -> 全程统一
 *   避免: 部分从回收池取, 部分从号段取 (连续性断裂 + 防重压力)
 *
 * 锁定期: recycle_lock_hours（默认 24h，由规则配置）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleStrategyService {

    private final CodeAllocationRepository codeAllocationRepository;

    @Value("${codecenter.lifecycle.recycle-lock-hours:24}")
    private int defaultLockHours;

    /**
     * 从回收池获取可复用的编码
     *
     * @return 实际可用数量（可能小于 count）
     */
    public List<CodeAllocation> acquireRecycled(String ruleId, int count, LocalDateTime now) {
        if (count <= 0) return List.of();
        // 一次尽量取够 count
        List<CodeAllocation> list = codeAllocationRepository.findRecyclableByRuleId(
                ruleId, now, PageRequest.of(0, count));
        log.debug("Acquire recycled: ruleId={}, requested={}, available={}", ruleId, count, list.size());
        return list;
    }

    /**
     * 复用 - 把 CANCELLED/RECYCLED 重新置为 PENDING 并清理锁定时间
     */
    @Transactional
    public List<CodeAllocation> reuse(List<CodeAllocation> allocations) {
        for (CodeAllocation a : allocations) {
            a.setStatus(AllocationStatus.PENDING);
            a.setCancelTime(null);
            a.setRecycleTime(null);
            a.setRecycleLockTime(null);
            a.setWasteType(null);
            a.setDataId(null);
        }
        return allocations;
    }

    /**
     * 把 USED 状态的编码进入回收池（业务数据删除时调用）
     */
    @Transactional
    public CodeAllocation enterRecyclePool(String code) {
        CodeAllocation a = codeAllocationRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new IllegalArgumentException("Code not found: " + code));
        if (a.getStatus() != AllocationStatus.USED) {
            throw new IllegalStateException("Only USED can enter recycle pool, current=" + a.getStatus());
        }
        a.recycle();
        a.setRecycleLockTime(LocalDateTime.now().plusHours(defaultLockHours));
        a.setDataId(null);
        log.info("Code entered recycle pool: code={}, lockUntil={}", code, a.getRecycleLockTime());
        return codeAllocationRepository.save(a);
    }

    /**
     * 定时任务 - 释放过期的回收码
     */
    @Transactional
    public int releaseExpiredLocks(LocalDateTime now) {
        // 实际实现: 找到 recycleLockTime <= now 的 RECYCLED 码，物理清理（可选）
        // 或改为软删除（is_archived = 1）
        // 此处保留接口以供定时任务调用
        return 0;
    }
}
