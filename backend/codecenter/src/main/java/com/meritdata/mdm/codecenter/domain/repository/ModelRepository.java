package com.meritdata.mdm.codecenter.domain.repository;

import com.meritdata.mdm.codecenter.domain.entity.Model;
import com.meritdata.mdm.codecenter.domain.enums.ModelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<Model, String> {
    Optional<Model> findByTenantIdAndModelCode(String tenantId, String modelCode);
    Optional<Model> findByTenantIdAndTableName(String tenantId, String tableName);
    List<Model> findByThemeId(String themeId);
    Page<Model> findByTenantIdAndStatus(String tenantId, ModelStatus status, Pageable pageable);
    List<Model> findByTenantIdOrderByCreateTimeDesc(String tenantId);
    Page<Model> findByTenantId(String tenantId, Pageable pageable);
}
