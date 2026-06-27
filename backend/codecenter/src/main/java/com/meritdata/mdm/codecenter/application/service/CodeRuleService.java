package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.application.cache.RuleCacheService;
import com.meritdata.mdm.codecenter.application.dto.CodeRuleRequest;
import com.meritdata.mdm.codecenter.common.exception.BizException;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.CodeRule;
import com.meritdata.mdm.codecenter.domain.entity.CodeRuleSegment;
import com.meritdata.mdm.codecenter.domain.enums.RuleStatus;
import com.meritdata.mdm.codecenter.domain.enums.RuleMode;
import com.meritdata.mdm.codecenter.domain.enums.GenerateTrigger;
import com.meritdata.mdm.codecenter.domain.repository.CodeRuleRepository;
import com.meritdata.mdm.codecenter.domain.repository.CodeRuleSegmentRepository;
import com.meritdata.mdm.codecenter.domain.valueobject.FormatTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 编码规则管理服务 - CRUD + 生命周期
 *
 * 缓存协同 (V0.9):
 *   - create / update / publish / revise / disable / enable / delete 后
 *     必须显式失效 RuleCacheService, 防止读到旧版本.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeRuleService {

    private final CodeRuleRepository codeRuleRepository;
    private final CodeRuleSegmentRepository codeRuleSegmentRepository;
    private final AuditLogService auditLogService;
    private final RuleCacheService ruleCacheService;

    @Transactional
    public CodeRule create(CodeRuleRequest req, String operatorId) {
        validate(req);
        CodeRule rule = CodeRule.builder()
                .id(IdUtil.simpleId())
                .modelId(req.getModelId())
                .encodeFieldId(req.getEncodeFieldId())
                .ruleName(req.getRuleName())
                .ruleCode(req.getRuleCode())
                .ruleDesc(req.getRuleDesc())
                .ruleMode(req.getRuleMode() == null ? RuleMode.DSL : req.getRuleMode())
                .triggerType(req.getTriggerType() == null ? GenerateTrigger.BUTTON : req.getTriggerType())
                .dslTemplate(req.getDslTemplate())
                .groovyScript(req.getGroovyScript())
                .version(1)
                .status(RuleStatus.EDIT)
                .recycleLockHours(req.getRecycleLockHours() == null ? 24 : req.getRecycleLockHours())
                .recycleStrategy(req.getRecycleStrategy() == null ? "AUTO" : req.getRecycleStrategy())
                .createdBy(operatorId)
                .createdAt(LocalDateTime.now())
                .tenantId(req.getTenantId())
                .build();
        CodeRule saved = codeRuleRepository.save(rule);
        if (req.getSegments() != null) {
            saveRuleSegments(saved, req.getSegments());
        }
        auditLogService.record(operatorId, null, "RULE_CREATE", saved.getId(), "RULE",
                null, RuleStatus.EDIT.name(), null);
        log.info("Code rule created: id={}, model={}, field={}",
                saved.getId(), saved.getModelId(), saved.getEncodeFieldId());
        return saved;
    }

    @Transactional
    public CodeRule update(String id, CodeRuleRequest req, String operatorId) {
        CodeRule rule = codeRuleRepository.findById(id).orElseThrow(() -> BizException.ruleNotFound(id));
        if (rule.getStatus() != RuleStatus.EDIT) {
            throw BizException.ruleStatusInvalid(rule.getStatus().name());
        }
        rule.setRuleName(req.getRuleName());
        rule.setRuleCode(req.getRuleCode());
        rule.setRuleDesc(req.getRuleDesc());
        rule.setDslTemplate(req.getDslTemplate());
        rule.setGroovyScript(req.getGroovyScript());
        rule.setRecycleLockHours(req.getRecycleLockHours() == null ? rule.getRecycleLockHours() : req.getRecycleLockHours());
        rule.setUpdatedBy(operatorId);
        rule.setUpdatedAt(LocalDateTime.now());
        CodeRule saved = codeRuleRepository.save(rule);
        if (req.getSegments() != null) {
            codeRuleSegmentRepository.deleteByRuleId(id);
            saveRuleSegments(saved, req.getSegments());
        }
        auditLogService.record(operatorId, null, "RULE_UPDATE", id, "RULE", null, null, null);
        // 缓存失效: 模板 / 码段可能都变了
        ruleCacheService.invalidate(saved.getModelId(), saved.getEncodeFieldId());
        return saved;
    }

    @Transactional
    public void delete(String id, String operatorId) {
        CodeRule rule = codeRuleRepository.findById(id).orElseThrow(() -> BizException.ruleNotFound(id));
        if (rule.getStatus() != RuleStatus.EDIT) {
            throw BizException.ruleStatusInvalid(rule.getStatus().name());
        }
        codeRuleSegmentRepository.deleteByRuleId(id);
        codeRuleRepository.deleteById(id);
        auditLogService.record(operatorId, null, "RULE_DELETE", id, "RULE",
                RuleStatus.EDIT.name(), null, null);
        ruleCacheService.invalidate(rule.getModelId(), rule.getEncodeFieldId());
    }

    @Transactional
    public CodeRule publish(String id, String operatorId) {
        CodeRule rule = codeRuleRepository.findById(id).orElseThrow(() -> BizException.ruleNotFound(id));
        if (rule.getStatus() != RuleStatus.EDIT) {
            throw BizException.ruleStatusInvalid(rule.getStatus().name());
        }
        if (rule.getDslTemplate() != null && !rule.getDslTemplate().isEmpty()) {
            try {
                FormatTemplate.parse(rule.getDslTemplate());
            } catch (Exception e) {
                throw new BizException("CODECENTER-RULE-1004",
                        "DSL template parse failed: " + e.getMessage());
            }
        }
        codeRuleRepository.findByModelIdAndStatus(rule.getModelId(), RuleStatus.EFFECT)
                .forEach(r -> {
                    r.setStatus(RuleStatus.HISTORY);
                    codeRuleRepository.save(r);
                });
        rule.publish();
        CodeRule saved = codeRuleRepository.save(rule);
        auditLogService.record(operatorId, null, "RULE_PUBLISH", id, "RULE",
                RuleStatus.EDIT.name(), RuleStatus.EFFECT.name(), null);
        log.info("Code rule published: id={}, version={}", id, saved.getVersion());
        // 缓存失效: 新 EFFECT 版本必须立即生效
        ruleCacheService.invalidate(saved.getModelId(), saved.getEncodeFieldId());
        return saved;
    }

    @Transactional
    public CodeRule revise(String id, String operatorId) {
        CodeRule old = codeRuleRepository.findById(id).orElseThrow(() -> BizException.ruleNotFound(id));
        if (old.getStatus() != RuleStatus.EFFECT) {
            throw BizException.ruleStatusInvalid(old.getStatus().name());
        }
        CodeRule newRule = CodeRule.builder()
                .id(IdUtil.simpleId())
                .modelId(old.getModelId())
                .encodeFieldId(old.getEncodeFieldId())
                .ruleName(old.getRuleName())
                .ruleCode(old.getRuleCode())
                .ruleDesc(old.getRuleDesc())
                .ruleMode(old.getRuleMode())
                .triggerType(old.getTriggerType())
                .dslTemplate(old.getDslTemplate())
                .groovyScript(old.getGroovyScript())
                .version(old.getVersion() + 1)
                .status(RuleStatus.EDIT)
                .recycleLockHours(old.getRecycleLockHours())
                .recycleStrategy(old.getRecycleStrategy())
                .createdBy(operatorId)
                .createdAt(LocalDateTime.now())
                .tenantId(old.getTenantId())
                .build();
        CodeRule saved = codeRuleRepository.save(newRule);
        codeRuleSegmentRepository.findByRuleIdOrderBySortOrderAsc(old.getId())
                .forEach(rs -> {
                    CodeRuleSegment copy = CodeRuleSegment.builder()
                            .id(IdUtil.simpleId())
                            .ruleId(saved.getId())
                            .segmentId(rs.getSegmentId())
                            .sortOrder(rs.getSortOrder())
                            .resetCondition(rs.getResetCondition())
                            .tenantId(rs.getTenantId())
                            .build();
                    codeRuleSegmentRepository.save(copy);
                });
        auditLogService.record(operatorId, null, "RULE_REVISE", saved.getId(), "RULE",
                null, RuleStatus.EDIT.name(), "from=" + old.getId());
        // 当前 EFFECT 仍是旧版本, 缓存不立即失效; 等 publish 才失效
        return saved;
    }

    @Transactional
    public CodeRule disable(String id, String operatorId) {
        CodeRule rule = codeRuleRepository.findById(id).orElseThrow(() -> BizException.ruleNotFound(id));
        rule.disable();
        CodeRule saved = codeRuleRepository.save(rule);
        auditLogService.record(operatorId, null, "RULE_DISABLE", id, "RULE",
                rule.getStatus().name(), RuleStatus.DISABLED.name(), null);
        ruleCacheService.invalidate(saved.getModelId(), saved.getEncodeFieldId());
        return saved;
    }

    @Transactional
    public CodeRule enable(String id, String operatorId) {
        CodeRule rule = codeRuleRepository.findById(id).orElseThrow(() -> BizException.ruleNotFound(id));
        rule.enable();
        CodeRule saved = codeRuleRepository.save(rule);
        auditLogService.record(operatorId, null, "RULE_ENABLE", id, "RULE",
                RuleStatus.DISABLED.name(), RuleStatus.EFFECT.name(), null);
        ruleCacheService.invalidate(saved.getModelId(), saved.getEncodeFieldId());
        return saved;
    }

    public Page<CodeRule> listByModel(String modelId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(size, 200));
        return codeRuleRepository.findByModelId(modelId, pageable);
    }

    public List<CodeRule> listVersions(String modelId, String fieldId) {
        return codeRuleRepository.findByModelIdAndEncodeFieldIdOrderByVersionDesc(modelId, fieldId);
    }

    public CodeRule getById(String id) {
        return codeRuleRepository.findById(id).orElseThrow(() -> BizException.ruleNotFound(id));
    }

    public CodeRule getEffective(String modelId, String fieldId) {
        return codeRuleRepository.findFirstByModelIdAndEncodeFieldIdAndStatusOrderByVersionDesc(
                modelId, fieldId, RuleStatus.EFFECT)
                .orElseThrow(() -> BizException.ruleNotFound("model=" + modelId + ",field=" + fieldId));
    }

    private void validate(CodeRuleRequest req) {
        if (req.getModelId() == null) throw BizException.paramInvalid("modelId");
        if (req.getEncodeFieldId() == null) throw BizException.paramInvalid("encodeFieldId");
        if (req.getRuleName() == null) throw BizException.paramInvalid("ruleName");
        if (req.getRuleCode() == null) throw BizException.paramInvalid("ruleCode");
        if (req.getDslTemplate() == null || req.getDslTemplate().isEmpty()) {
            throw BizException.paramInvalid("dslTemplate");
        }
    }

    private void saveRuleSegments(CodeRule rule, List<CodeRuleRequest.RuleSegmentRef> refs) {
        if (refs == null) return;
        for (CodeRuleRequest.RuleSegmentRef ref : refs) {
            CodeRuleSegment rs = CodeRuleSegment.builder()
                    .id(IdUtil.simpleId())
                    .ruleId(rule.getId())
                    .segmentId(ref.getSegmentId())
                    .sortOrder(ref.getSortOrder() == null ? 0 : ref.getSortOrder())
                    .resetCondition(ref.getResetCondition())
                    .tenantId(rule.getTenantId())
                    .build();
            codeRuleSegmentRepository.save(rs);
        }
    }
}
