package com.meritdata.mdm.codecenter.domain.repository;

import com.meritdata.mdm.codecenter.domain.entity.CodeAllocation;
import com.meritdata.mdm.codecenter.domain.enums.AllocationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodeAllocationRepository extends JpaRepository<CodeAllocation, String> {

    Optional<CodeAllocation> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * 排除某些状态的 active code 检查
     */
    @Query("SELECT COUNT(a) > 0 FROM CodeAllocation a " +
            "WHERE a.code = :code AND a.status NOT IN :excludedStatuses")
    boolean existsByCodeAndStatusNotIn(@Param("code") String code,
                                       @Param("excludedStatuses") Collection<AllocationStatus> excludedStatuses);

    /**
     * 检查 code 是否以 active 状态(PENDING/USED)存在 - 使用 native query + 字符串比较
     * 避免 Hibernate ENUM 类型绑定问题
     */
    @Query(value = "SELECT COUNT(*) FROM md_code_allocation " +
                   "WHERE code = :code AND (status = :s1 OR status = :s2)",
           nativeQuery = true)
    long countActiveByCodeNative(@Param("code") String code,
                                 @Param("s1") String status1,
                                 @Param("s2") String status2);

    /**
     * 列出 code 相关的所有分配（按 status 排查用）
     */
    List<CodeAllocation> findByCodeIn(Collection<String> codes);

    List<CodeAllocation> findByRuleIdAndStatus(String ruleId, AllocationStatus status);

    Page<CodeAllocation> findByRuleId(String ruleId, Pageable pageable);

    @Query("SELECT a FROM CodeAllocation a " +
            "WHERE a.ruleId = :ruleId " +
            "AND a.status IN (com.meritdata.mdm.codecenter.domain.enums.AllocationStatus.CANCELLED, " +
            "               com.meritdata.mdm.codecenter.domain.enums.AllocationStatus.RECYCLED) " +
            "AND (a.recycleLockTime IS NULL OR a.recycleLockTime <= :now) " +
            "ORDER BY a.recycleTime ASC")
    List<CodeAllocation> findRecyclableByRuleId(@Param("ruleId") String ruleId,
                                                @Param("now") LocalDateTime now,
                                                Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CodeAllocation a WHERE a.code = :code")
    Optional<CodeAllocation> findByCodeForUpdate(@Param("code") String code);

    @Query("SELECT a FROM CodeAllocation a " +
            "WHERE a.status = com.meritdata.mdm.codecenter.domain.enums.AllocationStatus.PENDING " +
            "AND a.allocateTime < :expireTime")
    List<CodeAllocation> findPendingExpired(@Param("expireTime") LocalDateTime expireTime,
                                            Pageable pageable);

    @Query("SELECT a.status, COUNT(a) FROM CodeAllocation a WHERE a.ruleId = :ruleId GROUP BY a.status")
    List<Object[]> countByRuleIdGroupByStatus(@Param("ruleId") String ruleId);

    @Query("SELECT MAX(a.sequenceNum) FROM CodeAllocation a WHERE a.ruleId = :ruleId")
    Optional<Long> findMaxSequenceNumByRuleId(@Param("ruleId") String ruleId);
}
