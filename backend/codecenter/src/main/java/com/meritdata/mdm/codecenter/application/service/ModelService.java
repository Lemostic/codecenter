package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.application.dto.ModelAttributeRequest;
import com.meritdata.mdm.codecenter.application.dto.ModelRequest;
import com.meritdata.mdm.codecenter.common.exception.BizException;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.Model;
import com.meritdata.mdm.codecenter.domain.entity.ModelAttribute;
import com.meritdata.mdm.codecenter.domain.enums.ModelStatus;
import com.meritdata.mdm.codecenter.domain.enums.ModelType;
import com.meritdata.mdm.codecenter.domain.repository.ModelAttributeRepository;
import com.meritdata.mdm.codecenter.domain.repository.ModelRepository;
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

/**
 * 模型服务 - 模型管理 / 模型元数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelRepository modelRepository;
    private final ModelAttributeRepository modelAttributeRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Model create(ModelRequest req, String operatorId) {
        validate(req);
        Model model = Model.builder()
                .id(IdUtil.simpleId())
                .modelCode(req.getModelCode())
                .modelName(req.getModelName())
                .tableName(req.getTableName())
                .modelType(req.getModelType() == null ? ModelType.NORMAL : req.getModelType())
                .themeId(req.getThemeId())
                .description(req.getDescription())
                .securityLevel(req.getSecurityLevel() == null ? "INTERNAL" : req.getSecurityLevel())
                .version(1)
                .status(ModelStatus.EDIT)
                .tenantId(req.getTenantId())
                .createBy(operatorId)
                .createTime(LocalDateTime.now())
                .build();
        Model saved = modelRepository.save(model);
        if (req.getAttributes() != null) {
            saveAttributes(saved, req.getAttributes());
        }
        auditLogService.record(operatorId, null, "MODEL_CREATE", saved.getId(), "MODEL",
                null, ModelStatus.EDIT.name(), null);
        log.info("Model created: id={}, code={}", saved.getId(), saved.getModelCode());
        return saved;
    }

    @Transactional
    public Model update(String id, ModelRequest req, String operatorId) {
        Model m = modelRepository.findById(id).orElseThrow(() -> BizException.modelNotFound(id));
        if (m.getStatus() != ModelStatus.EDIT) {
            throw BizException.modelStatusInvalid(m.getStatus().name());
        }
        m.setModelName(req.getModelName());
        m.setDescription(req.getDescription());
        m.setSecurityLevel(req.getSecurityLevel());
        m.setThemeId(req.getThemeId());
        m.setUpdateBy(operatorId);
        m.setUpdateTime(LocalDateTime.now());
        Model saved = modelRepository.save(m);
        if (req.getAttributes() != null) {
            modelAttributeRepository.findByModelIdOrderBySortOrderAsc(id)
                    .forEach(a -> modelAttributeRepository.deleteById(a.getId()));
            saveAttributes(saved, req.getAttributes());
        }
        auditLogService.record(operatorId, null, "MODEL_UPDATE", id, "MODEL", null, null, null);
        return saved;
    }

    @Transactional
    public void delete(String id, String operatorId) {
        Model m = modelRepository.findById(id).orElseThrow(() -> BizException.modelNotFound(id));
        if (m.getStatus() != ModelStatus.EDIT) {
            throw BizException.modelStatusInvalid(m.getStatus().name());
        }
        modelRepository.deleteById(id);
        auditLogService.record(operatorId, null, "MODEL_DELETE", id, "MODEL", null, null, null);
    }

    public Page<Model> list(String tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(size, 200));
        return modelRepository.findByTenantId(tenantId, pageable);
    }

    public Model getById(String id) {
        return modelRepository.findById(id).orElseThrow(() -> BizException.modelNotFound(id));
    }

    public List<ModelAttribute> listAttributes(String modelId) {
        return modelAttributeRepository.findByModelIdOrderBySortOrderAsc(modelId);
    }

    public List<ModelAttribute> listCodeFields(String modelId) {
        return modelAttributeRepository.findByModelIdAndIsCodeFieldTrue(modelId);
    }

    @Transactional
    public ModelAttribute addAttribute(String modelId, ModelAttributeRequest req, String operatorId) {
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
        auditLogService.record(operatorId, null, "MODEL_ATTR_CREATE", saved.getId(), "ATTRIBUTE",
                null, null, null);
        return saved;
    }

    private void validate(ModelRequest req) {
        if (req.getModelCode() == null) throw BizException.paramInvalid("modelCode");
        if (req.getModelName() == null) throw BizException.paramInvalid("modelName");
        if (req.getTableName() == null) throw BizException.paramInvalid("tableName");
        if (modelRepository.findByTenantIdAndModelCode(req.getTenantId(), req.getModelCode()).isPresent()) {
            throw new BizException("CODECENTER-MODEL-4005", "Model code already exists");
        }
        if (modelRepository.findByTenantIdAndTableName(req.getTenantId(), req.getTableName()).isPresent()) {
            throw new BizException("CODECENTER-MODEL-4006", "Table name already exists");
        }
    }

    private void saveAttributes(Model model, List<ModelAttributeRequest> attrs) {
        if (attrs == null || attrs.isEmpty()) return;
        for (ModelAttributeRequest req : attrs) {
            ModelAttribute attr = ModelAttribute.builder()
                    .id(req.getId() == null ? IdUtil.simpleId() : req.getId())
                    .modelId(model.getId())
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
                    .tenantId(model.getTenantId())
                    .build();
            modelAttributeRepository.save(attr);
        }
    }
}
