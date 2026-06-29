package com.meritdata.mdm.codecenter.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meritdata.mdm.codecenter.application.dto.CodeSegmentRequest;
import com.meritdata.mdm.codecenter.common.exception.BizException;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.domain.enums.SegmentType;
import com.meritdata.mdm.codecenter.domain.repository.CodeRuleSegmentRepository;
import com.meritdata.mdm.codecenter.domain.repository.CodeSegmentRepository;
import com.meritdata.mdm.codecenter.domain.valueobject.config.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 码段管理服务 - CRUD
 *
 * 8 种码段配置 (V0.3 - 6 类基础):
 *   FIXED / DATE / SEQUENCE / EIGENVALUE / REFERENCE / REFERENCE_SEQ
 *
 * 复合场景: 通过 DSL 组合多个原子码段实现（日期流水、动态流水、区间流水）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeSegmentService {

    private final CodeSegmentRepository codeSegmentRepository;
    private final CodeRuleSegmentRepository codeRuleSegmentRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CodeSegment create(CodeSegmentRequest req, String operatorId) {
        validate(req);
        CodeSegment seg = CodeSegment.builder()
                .id(IdUtil.simpleId())
                .segmentCode(req.getSegmentCode())
                .segmentName(req.getSegmentName())
                .segmentType(req.getSegmentType())
                .configJson(req.getConfigJson())
                .description(req.getDescription())
                .isArchived(false)
                .tenantId(req.getTenantId())
                .createdBy(operatorId)
                .createdAt(LocalDateTime.now())
                .build();
        CodeSegment saved = codeSegmentRepository.save(seg);
        auditLogService.record(operatorId, null, "SEGMENT_CREATE", saved.getId(), "SEGMENT",
                null, null, null);
        log.info("Code segment created: id={}, code={}, type={}",
                saved.getId(), saved.getSegmentCode(), saved.getSegmentType());
        return saved;
    }

    @Transactional
    public CodeSegment update(String id, CodeSegmentRequest req, String operatorId) {
        CodeSegment seg = codeSegmentRepository.findById(id).orElseThrow(() -> BizException.segmentNotFound(id));
        if (Boolean.TRUE.equals(seg.getIsArchived())) {
            throw BizException.segmentArchived(seg.getSegmentCode());
        }
        // 更新场景下，允许仅修改部分字段；configJson 为空时保留原值
        if (req.getConfigJson() == null || req.getConfigJson().isEmpty()) {
            validatePartial(req);
        } else {
            validate(req);
        }
        if (req.getSegmentCode() != null) seg.setSegmentCode(req.getSegmentCode());
        if (req.getSegmentName() != null) seg.setSegmentName(req.getSegmentName());
        if (req.getSegmentType() != null) seg.setSegmentType(req.getSegmentType());
        if (req.getConfigJson() != null && !req.getConfigJson().isEmpty()) seg.setConfigJson(req.getConfigJson());
        if (req.getDescription() != null) seg.setDescription(req.getDescription());
        seg.setUpdatedBy(operatorId);
        seg.setUpdatedAt(LocalDateTime.now());
        CodeSegment saved = codeSegmentRepository.save(seg);
        auditLogService.record(operatorId, null, "SEGMENT_UPDATE", id, "SEGMENT", null, null, null);
        return saved;
    }

    private void validatePartial(CodeSegmentRequest req) {
        // 只校验非空字段
        if (req.getSegmentCode() != null && req.getSegmentCode().isEmpty()) {
            throw BizException.paramInvalid("segmentCode");
        }
        if (req.getSegmentName() != null && req.getSegmentName().isEmpty()) {
            throw BizException.paramInvalid("segmentName");
        }
        if (req.getConfigJson() != null && !req.getConfigJson().isEmpty()) {
            try {
                objectMapper.readTree(req.getConfigJson());
            } catch (Exception e) {
                throw new BizException("CODECENTER-SEG-2004", "Invalid JSON: " + e.getMessage());
            }
        }
    }

    /**
     * 软删除 - 标记为归档
     */
    @Transactional
    public void archive(String id, String operatorId) {
        CodeSegment seg = codeSegmentRepository.findById(id).orElseThrow(() -> BizException.segmentNotFound(id));
        long referencedBy = codeRuleSegmentRepository.countBySegmentId(id);
        if (referencedBy > 0) {
            throw new BizException("CODECENTER-SEG-2003",
                    "Segment is referenced by " + referencedBy + " rules, cannot archive");
        }
        seg.setIsArchived(true);
        seg.setUpdatedBy(operatorId);
        seg.setUpdatedAt(LocalDateTime.now());
        codeSegmentRepository.save(seg);
        auditLogService.record(operatorId, null, "SEGMENT_ARCHIVE", id, "SEGMENT", null, "ARCHIVED", null);
        log.info("Code segment archived: id={}, code={}", id, seg.getSegmentCode());
    }

    @Transactional
    public void restore(String id, String operatorId) {
        CodeSegment seg = codeSegmentRepository.findById(id).orElseThrow(() -> BizException.segmentNotFound(id));
        seg.setIsArchived(false);
        seg.setUpdatedBy(operatorId);
        seg.setUpdatedAt(LocalDateTime.now());
        codeSegmentRepository.save(seg);
        auditLogService.record(operatorId, null, "SEGMENT_RESTORE", id, "SEGMENT", "ARCHIVED", null, null);
    }

    public Page<CodeSegment> list(SegmentType type, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return type == null
                ? codeSegmentRepository.findByTenantIdAndIsArchivedFalse(null, pageable)
                : codeSegmentRepository.findByTenantIdAndSegmentTypeAndIsArchivedFalse(null, type, pageable);
    }

    public CodeSegment getById(String id) {
        return codeSegmentRepository.findById(id).orElseThrow(() -> BizException.segmentNotFound(id));
    }

    /**
     * 解析并预览码段配置
     */
    public SegmentTypeConfig previewConfig(SegmentType type, String configJson) {
        return SegmentTypeConfig.parse(type, configJson);
    }

    private void validate(CodeSegmentRequest req) {
        if (req.getSegmentCode() == null || req.getSegmentCode().isEmpty()) {
            throw BizException.paramInvalid("segmentCode");
        }
        if (req.getSegmentName() == null || req.getSegmentName().isEmpty()) {
            throw BizException.paramInvalid("segmentName");
        }
        if (req.getSegmentType() == null) {
            throw BizException.paramInvalid("segmentType");
        }
        if (req.getConfigJson() == null || req.getConfigJson().isEmpty()) {
            throw BizException.paramInvalid("configJson");
        }
        try {
            objectMapper.readTree(req.getConfigJson());
        } catch (Exception e) {
            throw new BizException("CODECENTER-SEG-2004", "Invalid JSON: " + e.getMessage());
        }
        // 解析 config
        try {
            SegmentTypeConfig.parse(req.getSegmentType(), req.getConfigJson());
        } catch (Exception e) {
            throw new BizException("CODECENTER-SEG-2005",
                    "Config validation failed: " + e.getMessage());
        }
    }
}
