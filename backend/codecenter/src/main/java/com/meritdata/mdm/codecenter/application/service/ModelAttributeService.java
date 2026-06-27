package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.common.exception.BizException;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.ModelAttribute;
import com.meritdata.mdm.codecenter.domain.repository.ModelAttributeRepository;
import com.meritdata.mdm.codecenter.application.dto.ModelAttributeRequest;
import com.meritdata.mdm.codecenter.domain.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型属性（模型元数据）管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelAttributeService {

    private final ModelAttributeRepository modelAttributeRepository;
    private final ModelRepository modelRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ModelAttribute create(String modelId, ModelAttributeRequest req, String operatorId) {
        if (!modelRepository.existsById(modelId)) {
            throw BizException.modelNotFound(modelId);
        }
        ModelAttribute attr = ModelAttribute.builder()
                .id(IdUtil.simpleId())
                .modelId(modelId)
                .cnName(req.getCnName())
                .enName(req.getEnName())
                .dataType(req.getDataType())
                .dataLength(req.getDataLength())
                .decimalLength(req.getDecimalLength())
                .isRequired(Boolean.TRUE.equals(req.getIsRequired()))
                .isUnique(Boolean.TRUE.equals(req.getIsUnique()))
                .isCodeField(Boolean.TRUE.equals(req.getIsCodeField()))
                .defaultValue(req.getDefaultValue())
                .dictType(req.getDictType())
                .sortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder())
                .status("EDIT")
                .comment(req.getComment())
                .tenantId(req.getTenantId())
                .build();
        ModelAttribute saved = modelAttributeRepository.save(attr);
        auditLogService.record(operatorId, null, "ATTR_CREATE", saved.getId(), "ATTRIBUTE",
                null, null, null);
        return saved;
    }

    @Transactional
    public void delete(String attributeId, String operatorId) {
        ModelAttribute attr = modelAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new BizException("CODECENTER-MODEL-4010", "Attribute not found"));
        modelAttributeRepository.deleteById(attributeId);
        auditLogService.record(operatorId, null, "ATTR_DELETE", attributeId, "ATTRIBUTE",
                null, null, null);
    }

    public List<ModelAttribute> list(String modelId) {
        return modelAttributeRepository.findByModelIdOrderBySortOrderAsc(modelId);
    }

    public ModelAttribute get(String attributeId) {
        return modelAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new BizException("CODECENTER-MODEL-4010", "Attribute not found"));
    }
}

