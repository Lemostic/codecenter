package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.CodeWaterMark;
import com.meritdata.mdm.codecenter.domain.repository.CodeWaterMarkRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 号段水位服务.
 *
 * V0.9 (perf+):
 *   - claimNext 用 "UPDATE + SELECT" 两步, 1 个事务:
 *       UPDATE ... CASE WHEN :requested > current_water THEN :requested ELSE current_water + 1 END
 *       WHERE biz_tag = :bizTag
 *     UPDATE 隐式拿行级 X 锁, 串行化同 bizTag 并发请求; 同事务内的 SELECT
 *     直接读回新值 (read-your-own-writes), 1 round-trip 比 V0.8 的
 *     "SELECT FOR UPDATE + UPDATE" 少一次网络往返, 在高并发下也省一次 X 锁持有时间.
 *   - 行级 X 锁串行化同 bizTag 并发请求, 跨 bizTag 完全并行.
 *   - 返回值保证: > 所有已并发/历史分配的 seq.
 *   - 显式 Isolation.READ_COMMITTED, 兼容 MySQL 5.x 默认隔离级别.
 */
@Slf4j
@Service
public class WaterMarkService {

    private final CodeWaterMarkRepository waterMarkRepository;
    private final EntityManager entityManager;

    private final ConcurrentHashMap<String, Boolean> initialized = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public WaterMarkService(CodeWaterMarkRepository waterMarkRepository,
                            EntityManager entityManager) {
        this.waterMarkRepository = waterMarkRepository;
        this.entityManager = entityManager;
    }

    /**
     * 原子地"读-改-写"水位并返回实际可用的 seq.
     *
     * 关键并发保证:
     *   - UPDATE 拿行 X 锁, 串行化同 bizTag 请求.
     *   - CASE WHEN 既支持常规递增 (current+1) 也支持"水位跳跃" (calibrate / 回收复用).
     *   - 同事务内 SELECT 读到 UPDATE 刚写入的值 (read-your-own-writes).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long claimNext(String bizTag, long requestedSeq) {
        ensureExistsLocked(bizTag, "");
        // 1) 原子写: max(requested, current+1) 写回
        int updated = entityManager.createNativeQuery(
                "UPDATE md_code_water_mark " +
                "SET current_water = CASE WHEN :requested > current_water " +
                "                          THEN :requested " +
                "                          ELSE current_water + 1 END, " +
                "    last_allocate_time = :now " +
                "WHERE biz_tag = :bizTag")
                .setParameter("requested", requestedSeq)
                .setParameter("now", LocalDateTime.now())
                .setParameter("bizTag", bizTag)
                .executeUpdate();
        if (updated == 0) {
            // 行被并发删除? 极少发生, 抛错让调用方感知
            throw new IllegalStateException(
                    "Water mark row not found after ensureExists: bizTag=" + bizTag);
        }
        // 2) 同事务内读出新值 (read-your-own-writes), 不需要额外 X 锁
        Object v = entityManager.createNativeQuery(
                "SELECT current_water FROM md_code_water_mark WHERE biz_tag = :bizTag")
                .setParameter("bizTag", bizTag)
                .getSingleResult();
        long actual = ((Number) v).longValue();
        if (log.isDebugEnabled()) {
            log.debug("claimNext bizTag={} requested={} actual={}", bizTag, requestedSeq, actual);
        }
        return actual;
    }

    /**
     * 判断 seq 是否在历史水位之下 (即可能被历史数据占用, 需要重写)
     */
    public boolean isBelowWater(String bizTag, long seq) {
        try {
            Object result = entityManager.createNativeQuery(
                    "SELECT current_water FROM md_code_water_mark WHERE biz_tag = :bizTag")
                    .setParameter("bizTag", bizTag)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (result == null) {
                return false;
            }
            long wm = ((Number) result).longValue();
            if (log.isDebugEnabled()) {
                log.debug("isBelowWater bizTag={} seq={} water={} below={}", bizTag, seq, wm, seq <= wm);
            }
            return seq <= wm;
        } catch (Exception e) {
            log.warn("isBelowWater error, fallback to non-blocking: bizTag={}, err={}", bizTag, e.toString());
            return false;
        }
    }

    /**
     * 记录一次分配 (非重写, 仅当 seq > currentWater 时更新)
     */
    @Transactional
    public void recordAllocation(String bizTag, long seq) {
        ensureExistsLocked(bizTag, "");
        int updated = entityManager.createNativeQuery(
                "UPDATE md_code_water_mark SET current_water = :newWater, last_allocate_time = :now " +
                "WHERE biz_tag = :bizTag AND current_water < :newWater")
                .setParameter("newWater", seq)
                .setParameter("now", LocalDateTime.now())
                .setParameter("bizTag", bizTag)
                .executeUpdate();
        if (log.isDebugEnabled()) {
            log.debug("recordAllocation bizTag={} seq={} updated={}", bizTag, seq, updated);
        }
    }

    /**
     * 原子地"读-改-写"水位, 返回最终使用的 seq.
     * V0.9 之后仅作 claimNext 的别名, 保留以兼容旧调用方.
     */
    public long recordAndReturn(String bizTag, long seq) {
        return claimNext(bizTag, seq);
    }

    @Transactional
    public boolean calibrateWater(String bizTag, long newWater, String source) {
        if (newWater < 0) return false;
        ensureExistsLocked(bizTag, "");
        int updated = entityManager.createNativeQuery(
                "UPDATE md_code_water_mark SET current_water = :newWater, last_calibrate_time = :now, " +
                "calibrate_source = :source WHERE biz_tag = :bizTag AND current_water < :newWater")
                .setParameter("newWater", newWater)
                .setParameter("now", LocalDateTime.now())
                .setParameter("source", source)
                .setParameter("bizTag", bizTag)
                .executeUpdate();
        if (updated > 0) {
            log.info("Water calibrated: bizTag={}, new={}, source={}", bizTag, newWater, source);
        }
        return updated > 0;
    }

    @Transactional
    public void initWater(String bizTag, String ruleId, long startValue) {
        ensureExistsWithValue(bizTag, ruleId, startValue);
    }

    /**
     * 确保水位行已存在. 在 synchronized 块内调用, 仅一个线程真正执行 INSERT.
     * 调用方应在并发调 claimNext 之前预热 (initWater), 否则多个线程会
     * 各自的事务都看不到对方的未提交 INSERT.
     */
    public void ensureExistsLocked(String bizTag, String ruleId) {
        if (initialized.containsKey(bizTag)) {
            return;
        }
        Object lock = locks.computeIfAbsent(bizTag, k -> new Object());
        synchronized (lock) {
            if (initialized.containsKey(bizTag)) {
                return;
            }
            doInsertInCurrentTx(bizTag, ruleId);
            initialized.put(bizTag, Boolean.TRUE);
        }
    }

    private void doInsertInCurrentTx(String bizTag, String ruleId) {
        if (waterMarkRepository.existsByBizTag(bizTag)) {
            return;
        }
        CodeWaterMark mark = CodeWaterMark.builder()
                .id(IdUtil.simpleId())
                .bizTag(bizTag)
                .ruleId(ruleId == null ? "" : ruleId)
                .currentWater(0L)
                .build();
        try {
            waterMarkRepository.saveAndFlush(mark);
            log.info("Water mark initialized: bizTag={}", bizTag);
        } catch (RuntimeException e) {
            if (isUniqueViolation(e)) {
                log.debug("Water mark race lost, continuing: bizTag={}", bizTag);
            } else {
                log.warn("Water mark init unexpected error: bizTag={}", bizTag, e);
            }
        }
    }

    /**
     * 兼容旧调用方的入口. 显式 @Transactional(REQUIRES_NEW), 必须由外部
     * Spring 代理调用.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doEnsureExists(String bizTag, String ruleId) {
        doInsertInCurrentTx(bizTag, ruleId);
    }

    private boolean isUniqueViolation(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && (msg.contains("uk_biz_tag") || msg.contains("Unique index")
                    || msg.contains("unique constraint") || msg.contains("UNIQUE"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    @Transactional
    public void ensureExistsWithValue(String bizTag, String ruleId, long startValue) {
        ensureExistsLocked(bizTag, ruleId);
        entityManager.createNativeQuery(
                "UPDATE md_code_water_mark SET current_water = :val, calibrate_source = 'PUBLISH' " +
                "WHERE biz_tag = :bizTag")
                .setParameter("val", startValue)
                .setParameter("bizTag", bizTag)
                .executeUpdate();
    }
}
