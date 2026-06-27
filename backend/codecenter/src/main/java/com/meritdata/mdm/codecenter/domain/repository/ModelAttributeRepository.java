package com.meritdata.mdm.codecenter.domain.repository;

import com.meritdata.mdm.codecenter.domain.entity.ModelAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelAttributeRepository extends JpaRepository<ModelAttribute, String> {
    List<ModelAttribute> findByModelIdOrderBySortOrderAsc(String modelId);
    List<ModelAttribute> findByModelIdAndIsCodeFieldTrue(String modelId);
    Optional<ModelAttribute> findByModelIdAndEnName(String modelId, String enName);
    long countByModelId(String modelId);
}
