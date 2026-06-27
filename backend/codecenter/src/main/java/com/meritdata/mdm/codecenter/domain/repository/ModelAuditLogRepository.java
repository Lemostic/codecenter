package com.meritdata.mdm.codecenter.domain.repository;

import com.meritdata.mdm.codecenter.domain.entity.ModelAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelAuditLogRepository extends JpaRepository<ModelAuditLog, String> {
    Page<ModelAuditLog> findByTargetIdAndTargetTypeOrderByOperatedAtDesc(String targetId, String targetType, Pageable pageable);
    Page<ModelAuditLog> findByTargetTypeOrderByOperatedAtDesc(String targetType, Pageable pageable);
}
