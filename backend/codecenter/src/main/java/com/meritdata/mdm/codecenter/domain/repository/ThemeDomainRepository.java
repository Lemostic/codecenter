package com.meritdata.mdm.codecenter.domain.repository;

import com.meritdata.mdm.codecenter.domain.entity.ThemeDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThemeDomainRepository extends JpaRepository<ThemeDomain, String> {
    Optional<ThemeDomain> findByTenantIdAndDomainCode(String tenantId, String domainCode);
    List<ThemeDomain> findByTenantIdAndParentIdIsNullOrderBySortOrderAsc(String tenantId);
    List<ThemeDomain> findByTenantIdAndParentIdOrderBySortOrderAsc(String tenantId, String parentId);
    List<ThemeDomain> findByTenantIdOrderBySortOrderAsc(String tenantId);
}
