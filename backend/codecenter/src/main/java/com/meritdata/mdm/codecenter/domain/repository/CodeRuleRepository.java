package com.meritdata.mdm.codecenter.domain.repository;

import com.meritdata.mdm.codecenter.domain.entity.CodeRule;
import com.meritdata.mdm.codecenter.domain.enums.RuleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeRuleRepository extends JpaRepository<CodeRule, String> {
    Optional<CodeRule> findByModelIdAndEncodeFieldIdAndVersion(String modelId, String encodeFieldId, Integer version);
    List<CodeRule> findByModelIdAndEncodeFieldIdOrderByVersionDesc(String modelId, String encodeFieldId);
    Page<CodeRule> findByModelId(String modelId, Pageable pageable);
    List<CodeRule> findByModelIdAndStatus(String modelId, RuleStatus status);
    Optional<CodeRule> findFirstByModelIdAndEncodeFieldIdAndStatusOrderByVersionDesc(String modelId, String encodeFieldId, RuleStatus status);
    List<CodeRule> findByStatus(RuleStatus status);
}
