package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.application.cache.RuleCacheService;
import com.meritdata.mdm.codecenter.application.dto.CodeRuleRequest;
import com.meritdata.mdm.codecenter.common.exception.BizException;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.CodeRule;
import com.meritdata.mdm.codecenter.domain.entity.CodeRuleSegment;
import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.RuleStatus;
import com.meritdata.mdm.codecenter.domain.enums.RuleMode;
import com.meritdata.mdm.codecenter.domain.enums.GenerateTrigger;
import com.meritdata.mdm.codecenter.domain.repository.CodeRuleRepository;
import com.meritdata.mdm.codecenter.domain.repository.CodeRuleSegmentRepository;
import com.meritdata.mdm.codecenter.domain.repository.CodeSegmentRepository;
import com.meritdata.mdm.codecenter.domain.valueobject.FormatTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final CodeSegmentRepository codeSegmentRepository;
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
        // PATCH 风格：null 字段不覆盖
        if (req.getRuleName() != null) rule.setRuleName(req.getRuleName());
        if (req.getRuleCode() != null) rule.setRuleCode(req.getRuleCode());
        if (req.getRuleDesc() != null) rule.setRuleDesc(req.getRuleDesc());
        if (req.getDslTemplate() != null) rule.setDslTemplate(req.getDslTemplate());
        if (req.getGroovyScript() != null) rule.setGroovyScript(req.getGroovyScript());
        if (req.getEncodeFieldId() != null) rule.setEncodeFieldId(req.getEncodeFieldId());
        if (req.getRuleMode() != null) rule.setRuleMode(req.getRuleMode());
        if (req.getTriggerType() != null) rule.setTriggerType(req.getTriggerType());
        if (req.getRecycleLockHours() != null) rule.setRecycleLockHours(req.getRecycleLockHours());
        if (req.getRecycleStrategy() != null) rule.setRecycleStrategy(req.getRecycleStrategy());
        rule.setUpdatedBy(operatorId);
        rule.setUpdatedAt(LocalDateTime.now());
        CodeRule saved = codeRuleRepository.save(rule);
        if (req.getSegments() != null) {
            codeRuleSegmentRepository.deleteByRuleId(id);
            codeRuleSegmentRepository.flush();
            saveRuleSegments(saved, req.getSegments());
        }
        auditLogService.record(operatorId, null, "RULE_UPDATE", id, "RULE", null, null, null);
        // 缓存失效: 模板 / 码段可能都变了
        ruleCacheService.invalidate(saved.getModelId(), saved.getEncodeFieldId());
        // 触发 lazy 初始化，避免 Controller 序列化时 Session 已关闭
        saved.getRuleSegments().size();
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
        // 触发 lazy 初始化，避免 Controller 序列化时 Session 已关闭
        saved.getRuleSegments().size();
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

@Transactional(readOnly = true)
    public Page<CodeRule> listByModel(String modelId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(size, 200));
        Page<CodeRule> p = codeRuleRepository.findByModelId(modelId, pageable);
        // 触发 lazy 初始化，避免 Controller 序列化时 Session 已关闭
        p.getContent().forEach(r -> r.getRuleSegments().size());
        return p;
    }

    @Transactional(readOnly = true)
    public List<CodeRule> listVersions(String modelId, String fieldId) {
        List<CodeRule> list = codeRuleRepository.findByModelIdAndEncodeFieldIdOrderByVersionDesc(modelId, fieldId);
        list.forEach(r -> r.getRuleSegments().size());
        return list;
    }

    @Transactional(readOnly = true)
    public CodeRule getById(String id) {
        CodeRule rule = codeRuleRepository.findById(id).orElseThrow(() -> BizException.ruleNotFound(id));
        rule.getRuleSegments().size();
        return rule;
    }

    @Transactional(readOnly = true)
    public CodeRule getEffective(String modelId, String fieldId) {
        CodeRule rule = codeRuleRepository.findFirstByModelIdAndEncodeFieldIdAndStatusOrderByVersionDesc(
                modelId, fieldId, RuleStatus.EFFECT)
                .orElseThrow(() -> BizException.ruleNotFound("model=" + modelId + ",field=" + fieldId));
        rule.getRuleSegments().size();
        return rule;
    }

    /**
     * 保存规则的码段配置 —— 替换式（先清空再插入）
     * @param ruleId 规则 ID
     * @param segments 前端传入的码段配置列表（含 segmentCode / segmentType / configJson / sortOrder）
     * @return 已保存的关联记录
     */
    @Transactional
    public List<CodeRuleSegment> saveRuleSegments(String ruleId, List<Map<String, Object>> segments) {
        CodeRule rule = codeRuleRepository.findById(ruleId)
                .orElseThrow(() -> BizException.ruleNotFound(ruleId));
        // 先清空
        codeRuleSegmentRepository.deleteByRuleId(ruleId);
        codeRuleSegmentRepository.flush();

        List<CodeRuleSegment> saved = new ArrayList<>();
        if (segments == null) {
            ruleCacheService.invalidate(rule.getModelId(), rule.getEncodeFieldId());
            auditLogService.record("system", null, "RULE_SEGMENTS_SAVE", ruleId, "RULE", null, null, null);
            return saved;
        }

        for (Map<String, Object> s : segments) {
            String segmentCode = (String) s.get("segmentCode");
            if (segmentCode == null || segmentCode.isEmpty()) continue;
            // 优先按 id 查，否则按 code 查
            CodeSegment seg = null;
            Object idObj = s.get("id");
            if (idObj != null) {
                seg = codeSegmentRepository.findById(idObj.toString()).orElse(null);
            }
            if (seg == null) {
                seg = codeSegmentRepository.findFirstBySegmentCodeAndIsArchivedFalse(segmentCode).orElse(null);
            }
            if (seg == null) {
                throw BizException.paramInvalid("segmentCode: " + segmentCode + " not found");
            }
            Integer sortOrder = s.get("sortOrder") == null ? saved.size() + 1 :
                    ((Number) s.get("sortOrder")).intValue();
            String resetCondition = (String) s.get("resetCondition");
            CodeRuleSegment link = CodeRuleSegment.builder()
                    .id(IdUtil.simpleId())
                    .ruleId(ruleId)
                    .segmentId(seg.getId())
                    .sortOrder(sortOrder)
                    .resetCondition(resetCondition)
                    .tenantId(rule.getTenantId())
                    .build();
            saved.add(codeRuleSegmentRepository.save(link));
        }
        ruleCacheService.invalidate(rule.getModelId(), rule.getEncodeFieldId());
        auditLogService.record("system", null, "RULE_SEGMENTS_SAVE", ruleId, "RULE",
                String.valueOf(saved.size()), null, null);
        log.info("Rule segments saved: ruleId={}, count={}", ruleId, saved.size());
        return saved;
    }

    /**
     * 获取规则的码段快照（包含 CodeSegment 详情）
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRuleSegments(String ruleId) {
        List<CodeRuleSegment> links = codeRuleSegmentRepository.findByRuleIdOrderBySortOrderAsc(ruleId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CodeRuleSegment link : links) {
            CodeSegment seg = codeSegmentRepository.findById(link.getSegmentId()).orElse(null);
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("linkId", link.getId());
            item.put("segmentId", link.getSegmentId());
            item.put("segmentCode", seg != null ? seg.getSegmentCode() : "");
            item.put("segmentName", seg != null ? seg.getSegmentName() : "");
            item.put("segmentType", seg != null ? seg.getSegmentType().name() : "");
            item.put("configJson", seg != null ? seg.getConfigJson() : "{}");
            item.put("sortOrder", link.getSortOrder());
            item.put("resetCondition", link.getResetCondition());
            result.add(item);
        }
        return result;
    }

    private void validate(CodeRuleRequest req) {
        if (req.getModelId() == null) throw BizException.paramInvalid("modelId");
        if (req.getEncodeFieldId() == null) throw BizException.paramInvalid("encodeFieldId");
        if (req.getRuleName() == null) throw BizException.paramInvalid("ruleName");
        if (req.getRuleCode() == null) throw BizException.paramInvalid("ruleCode");
        // dslTemplate / groovyScript 由前端按 ruleMode 选择性提交, 发布时才校验格式
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
