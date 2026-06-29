package com.meritdata.mdm.codecenter.domain.repository;

import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeSegmentRepository extends JpaRepository<CodeSegment, String> {
    Optional<CodeSegment> findByTenantIdAndSegmentCode(String tenantId, String segmentCode);
    Optional<CodeSegment> findByTenantIdAndSegmentName(String tenantId, String segmentName);
    Page<CodeSegment> findByTenantIdAndIsArchivedFalse(String tenantId, Pageable pageable);
    Page<CodeSegment> findByTenantIdAndSegmentTypeAndIsArchivedFalse(String tenantId, SegmentType type, Pageable pageable);
    List<CodeSegment> findByIdIn(List<String> ids);
    long countByIsArchivedFalse();

    Optional<CodeSegment> findFirstBySegmentCodeAndIsArchivedFalse(String segmentCode);
}
