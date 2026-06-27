package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.common.exception.BizException;
import com.meritdata.mdm.codecenter.domain.entity.CodeAllocation;
import com.meritdata.mdm.codecenter.domain.enums.AllocationStatus;
import com.meritdata.mdm.codecenter.domain.enums.WasteType;
import com.meritdata.mdm.codecenter.domain.repository.CodeAllocationRepository;
import com.meritdata.mdm.codecenter.infrastructure.dedup.CodeDedupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 编码生命周期服务（V0.3 - 4 态）
 *
 *   PENDING    - 预占中
 *   USED       - 已使用
 *   CANCELLED  - 已取消（可能进入回收池）
 *   RECYCLED   - 已回收（业务数据删除）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifecycleService {

    private final CodeAllocationRepository codeAllocationRepository;
    private final CodeDedupService codeDedupService;
    private final RecycleStrategyService recycleStrategyService;

    @Value("${codecenter.lifecycle.pending-timeout-minutes:30}")
    private int pendingTimeoutMinutes;

    @Value("${codecenter.lifecycle.recycle-lock-hours:24}")
    private int defaultRecycleLockHours;

    /**
     * 确认使用：PENDING -> USED
     */
    @Transactional
    public CodeAllocation confirm(String code, String operatorId) {
        CodeAllocation allocation = codeAllocationRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> BizException.codeAlreadyExists(code));
        if (allocation.getStatus() != AllocationStatus.PENDING) {
            throw new BizException("CODECENTER-CODE-3004",
                    "Only PENDING can be confirmed, current=" + allocation.getStatus());
        }
        allocation.confirm();
        log.info("Code confirmed: code={}, operator={}", code, operatorId);
        return codeAllocationRepository.save(allocation);
    }

    /**
     * 取消：PENDING -> CANCELLED
     *
     * isExposed = 0 (未暴露): 立即进入回收池
     * isExposed = 1 (已暴露): 进入"已取消"状态（不再回收到池，标记为浪费）
     */
    @Transactional
    public CodeAllocation cancel(String code, WasteType wasteType, String operatorId) {
        CodeAllocation allocation = codeAllocationRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> BizException.codeAlreadyExists(code));
        if (allocation.getStatus() != AllocationStatus.PENDING) {
            throw new BizException("CODECENTER-CODE-3005",
                    "Only PENDING can be cancelled, current=" + allocation.getStatus());
        }
        allocation.cancel(wasteType);
        if (Integer.valueOf(0).equals(allocation.getIsExposed())) {
            // 未暴露 - 进入回收池
            allocation.setRecycleTime(LocalDateTime.now());
            allocation.setRecycleLockTime(LocalDateTime.now().plusHours(defaultRecycleLockHours));
        }
        codeAllocationRepository.save(allocation);
        // 已暴露的码释放去重锁
        if (Integer.valueOf(1).equals(allocation.getIsExposed())) {
            codeDedupService.release(code);
        }
        log.info("Code cancelled: code={}, wasteType={}, exposed={}, operator={}",
                code, wasteType, allocation.getIsExposed(), operatorId);
        return allocation;
    }

    /**
     * 业务数据删除时回收
     */
    @Transactional
    public CodeAllocation recycleByDataId(String dataId) {
        List<CodeAllocation> allocations = codeAllocationRepository
                .findAll().stream()
                .filter(a -> dataId != null && dataId.equals(a.getDataId()))
                .toList();
        CodeAllocation result = null;
        for (CodeAllocation a : allocations) {
            if (a.getStatus() == AllocationStatus.USED) {
                recycleStrategyService.enterRecyclePool(a.getCode());
                result = a;
            } else if (a.getStatus() == AllocationStatus.PENDING) {
                cancel(a.getCode(), WasteType.DELETE, "system");
                result = a;
            }
        }
        return result;
    }

    /**
     * 扫描并回收 PENDING 超时的编码
     */
    @Transactional
    public int scanAndRecycleTimeout() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(pendingTimeoutMinutes);
        List<CodeAllocation> expired = codeAllocationRepository.findPendingExpired(expireTime,
                org.springframework.data.domain.PageRequest.of(0, 1000));
        int count = 0;
        for (CodeAllocation a : expired) {
            try {
                cancel(a.getCode(), WasteType.TIMEOUT, "system");
                count++;
            } catch (Exception e) {
                log.warn("Recycle timeout failed: code={}, error={}", a.getCode(), e.getMessage());
            }
        }
        if (count > 0) {
            log.info("PENDING timeout scan: recycled {} codes", count);
        }
        return count;
    }
}
