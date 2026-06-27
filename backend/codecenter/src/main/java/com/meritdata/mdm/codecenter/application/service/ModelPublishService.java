package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.common.exception.BizException;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.Model;
import com.meritdata.mdm.codecenter.domain.entity.ModelAttribute;
import com.meritdata.mdm.codecenter.domain.enums.ModelStatus;
import com.meritdata.mdm.codecenter.domain.enums.RuleStatus;
import com.meritdata.mdm.codecenter.domain.repository.CodeRuleRepository;
import com.meritdata.mdm.codecenter.domain.repository.ModelAttributeRepository;
import com.meritdata.mdm.codecenter.domain.repository.ModelRepository;
import com.meritdata.mdm.codecenter.infrastructure.dbscript.PhysicalTablePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模型发布服务
 *
 * 流程:
 *   1. 校验表名唯一 + 字段匹配
 *   2. 创建物理表 (跨库 DDL 适配)
 *   3. 状态: EDIT -> EFFECT
 *   4. 历史版本降为 HISTORY
 *   5. 写审计日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelPublishService {

    private final ModelRepository modelRepository;
    private final ModelAttributeRepository modelAttributeRepository;
    private final CodeRuleRepository codeRuleRepository;
    private final PhysicalTablePublisher physicalTablePublisher;
    private final AuditLogService auditLogService;

    @Transactional
    public Model publish(String modelId, boolean publishPhysicalTable, String operatorId) {
        Model model = modelRepository.findById(modelId).orElseThrow(() -> BizException.modelNotFound(modelId));
        if (model.getStatus() == ModelStatus.EFFECT) {
            throw new BizException("CODECENTER-MODEL-4007", "Model already EFFECT");
        }
        if (model.getStatus() == ModelStatus.DISABLED) {
            throw BizException.modelStatusInvalid(model.getStatus().name());
        }

        // Step 1: 校验表名（表已存在且未指定物理表发布则报错）
        boolean tableExists = physicalTablePublisher.tableExists(model.getTableName());
        if (tableExists && !publishPhysicalTable) {
            throw new BizException("CODECENTER-MODEL-4008",
                    "Physical table already exists: " + model.getTableName()
                            + ", please confirm publish mode");
        }

        // Step 2: 发布物理表
        if (publishPhysicalTable && !tableExists) {
            List<ModelAttribute> attributes = modelAttributeRepository
                    .findByModelIdOrderBySortOrderAsc(modelId);
            List<PhysicalTablePublisher.ColumnDef> columns = new ArrayList<>();
            for (ModelAttribute a : attributes) {
                PhysicalTablePublisher.ColumnDef col = new PhysicalTablePublisher.ColumnDef();
                col.setCnName(a.getCnName());
                col.setEnName(a.getEnName());
                col.setDataType(a.getDataType());
                col.setDataLength(a.getDataLength());
                col.setDecimalLength(a.getDecimalLength());
                col.setIsRequired(a.getIsRequired());
                col.setIsUnique(a.getIsUnique());
                col.setIsCodeField(a.getIsCodeField());
                columns.add(col);
            }
            physicalTablePublisher.createTable(model.getModelCode(), model.getTableName(), columns);
        }

        // Step 3: 历史版本降级
        modelRepository.findByTenantIdOrderByCreateTimeDesc(model.getTenantId())
                .stream()
                .filter(m -> m.getModelCode().equals(model.getModelCode())
                        && m.getStatus() == ModelStatus.EFFECT)
                .forEach(m -> {
                    m.setStatus(ModelStatus.HISTORY);
                    modelRepository.save(m);
                });

        // Step 4: 状态变更
        String before = model.getStatus().name();
        model.setStatus(ModelStatus.EFFECT);
        model.setUpdateBy(operatorId);
        model.setUpdateTime(LocalDateTime.now());
        Model saved = modelRepository.save(model);
        auditLogService.record(operatorId, null, "MODEL_PUBLISH", modelId, "MODEL",
                before, ModelStatus.EFFECT.name(),
                "table=" + model.getTableName() + ",physical=" + publishPhysicalTable);
        log.info("Model published: id={}, code={}, table={}, dialect={}",
                modelId, model.getModelCode(), model.getTableName(),
                physicalTablePublisher.detectDialect());
        return saved;
    }

    @Transactional
    public Model disable(String modelId, String operatorId) {
        Model model = modelRepository.findById(modelId).orElseThrow(() -> BizException.modelNotFound(modelId));
        if (model.getStatus() != ModelStatus.EFFECT) {
            throw BizException.modelStatusInvalid(model.getStatus().name());
        }
        // 同步停用关联规则
        codeRuleRepository.findByModelIdAndStatus(modelId, RuleStatus.EFFECT)
                .forEach(r -> {
                    r.setStatus(RuleStatus.DISABLED);
                    r.setDisabledAt(LocalDateTime.now());
                    codeRuleRepository.save(r);
                });
        model.setStatus(ModelStatus.DISABLED);
        model.setUpdateBy(operatorId);
        model.setUpdateTime(LocalDateTime.now());
        Model saved = modelRepository.save(model);
        auditLogService.record(operatorId, null, "MODEL_DISABLE", modelId, "MODEL",
                ModelStatus.EFFECT.name(), ModelStatus.DISABLED.name(), null);
        return saved;
    }

    @Transactional
    public Model enable(String modelId, String operatorId) {
        Model model = modelRepository.findById(modelId).orElseThrow(() -> BizException.modelNotFound(modelId));
        if (model.getStatus() != ModelStatus.DISABLED) {
            throw BizException.modelStatusInvalid(model.getStatus().name());
        }
        model.setStatus(ModelStatus.EFFECT);
        model.setUpdateBy(operatorId);
        model.setUpdateTime(LocalDateTime.now());
        Model saved = modelRepository.save(model);
        auditLogService.record(operatorId, null, "MODEL_ENABLE", modelId, "MODEL",
                ModelStatus.DISABLED.name(), ModelStatus.EFFECT.name(), null);
        return saved;
    }

    public String detectDialect() {
        return physicalTablePublisher.detectDialect();
    }
}
