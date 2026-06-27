package com.meritdata.mdm.codecenter.domain.repository;

import com.meritdata.mdm.codecenter.domain.entity.CodeWaterMark;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodeWaterMarkRepository extends JpaRepository<CodeWaterMark, String> {

    Optional<CodeWaterMark> findByBizTag(String bizTag);

    boolean existsByBizTag(String bizTag);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM CodeWaterMark w WHERE w.bizTag = :bizTag")
    Optional<CodeWaterMark> findByBizTagForUpdate(@Param("bizTag") String bizTag);
}
