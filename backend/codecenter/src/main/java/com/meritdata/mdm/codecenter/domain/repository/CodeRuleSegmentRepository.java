package com.meritdata.mdm.codecenter.domain.repository;

import com.meritdata.mdm.codecenter.domain.entity.CodeRuleSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeRuleSegmentRepository extends JpaRepository<CodeRuleSegment, String> {
    List<CodeRuleSegment> findByRuleIdOrderBySortOrderAsc(String ruleId);
    void deleteByRuleId(String ruleId);
    long countBySegmentId(String segmentId);
}
