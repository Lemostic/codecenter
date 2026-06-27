package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.application.dto.ThemeDomainRequest;
import com.meritdata.mdm.codecenter.common.exception.BizException;
import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.ThemeDomain;
import com.meritdata.mdm.codecenter.domain.repository.ModelRepository;
import com.meritdata.mdm.codecenter.domain.repository.ThemeDomainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 主题域服务
 *
 * 业务规则:
 *   - 名称 50 字符内, 仅汉字/字母/数字/-/_, 符号不可位于首位
 *   - 同父节点下不可重复 (大小写敏感)
 *   - 排序号默认 = 当前父节点最大序号 + 1
 *   - 删除: 不能存在子节点 / 不能挂载模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThemeDomainService {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9_\\-].*[\\u4e00-\\u9fa5A-Za-z0-9_\\-]$|^[\\u4e00-\\u9fa5A-Za-z0-9_\\-]$");

    private final ThemeDomainRepository themeDomainRepository;
    private final ModelRepository modelRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ThemeDomain create(ThemeDomainRequest req, String operatorId) {
        validateName(req.getDomainName());
        if (req.getSortOrder() == null) {
            List<ThemeDomain> siblings = req.getParentId() == null
                    ? themeDomainRepository.findByTenantIdAndParentIdIsNullOrderBySortOrderAsc(req.getTenantId())
                    : themeDomainRepository.findByTenantIdAndParentIdOrderBySortOrderAsc(req.getTenantId(), req.getParentId());
            int maxOrder = siblings.stream().mapToInt(ThemeDomain::getSortOrder).max().orElse(0);
            req.setSortOrder(maxOrder + 1);
        }
        if (req.getDomainCode() != null) {
            themeDomainRepository.findByTenantIdAndDomainCode(req.getTenantId(), req.getDomainCode())
                    .ifPresent(d -> { throw new BizException("CODECENTER-THEME-6001",
                            "Domain code already exists: " + req.getDomainCode()); });
        }
        ThemeDomain domain = ThemeDomain.builder()
                .id(IdUtil.simpleId())
                .parentId(req.getParentId())
                .domainCode(req.getDomainCode())
                .domainName(req.getDomainName())
                .sortOrder(req.getSortOrder())
                .remark(req.getRemark())
                .tenantId(req.getTenantId())
                .createBy(operatorId)
                .createTime(LocalDateTime.now())
                .build();
        ThemeDomain saved = themeDomainRepository.save(domain);
        auditLogService.record(operatorId, null, "THEME_CREATE", saved.getId(), "THEME",
                null, null, null);
        return saved;
    }

    @Transactional
    public ThemeDomain update(String id, ThemeDomainRequest req, String operatorId) {
        ThemeDomain d = themeDomainRepository.findById(id).orElseThrow(
                () -> new BizException("CODECENTER-THEME-6002", "Theme domain not found: " + id));
        validateName(req.getDomainName());
        d.setDomainCode(req.getDomainCode());
        d.setDomainName(req.getDomainName());
        d.setSortOrder(req.getSortOrder());
        d.setRemark(req.getRemark());
        d.setUpdateBy(operatorId);
        d.setUpdateTime(LocalDateTime.now());
        ThemeDomain saved = themeDomainRepository.save(d);
        auditLogService.record(operatorId, null, "THEME_UPDATE", id, "THEME", null, null, null);
        return saved;
    }

    @Transactional
    public void delete(String id, String operatorId) {
        ThemeDomain d = themeDomainRepository.findById(id).orElseThrow(
                () -> new BizException("CODECENTER-THEME-6002", "Theme domain not found: " + id));
        // 检查子节点
        List<ThemeDomain> children = themeDomainRepository
                .findByTenantIdAndParentIdOrderBySortOrderAsc(d.getTenantId(), id);
        if (!children.isEmpty()) {
            throw new BizException("CODECENTER-THEME-6003",
                    "Domain has " + children.size() + " children, delete them first");
        }
        // 检查模型引用
        List<com.meritdata.mdm.codecenter.domain.entity.Model> models = modelRepository.findByThemeId(id);
        if (!models.isEmpty()) {
            throw new BizException("CODECENTER-THEME-6004",
                    "Domain has " + models.size() + " models attached, move them first");
        }
        themeDomainRepository.deleteById(id);
        auditLogService.record(operatorId, null, "THEME_DELETE", id, "THEME", null, null, null);
    }

    public List<ThemeDomain> tree(String tenantId) {
        return themeDomainRepository.findByTenantIdOrderBySortOrderAsc(tenantId);
    }

    public List<ThemeDomain> children(String tenantId, String parentId) {
        return parentId == null
                ? themeDomainRepository.findByTenantIdAndParentIdIsNullOrderBySortOrderAsc(tenantId)
                : themeDomainRepository.findByTenantIdAndParentIdOrderBySortOrderAsc(tenantId, parentId);
    }

    private void validateName(String name) {
        if (name == null || name.isEmpty()) throw BizException.paramInvalid("domainName");
        if (name.length() > 50) throw new BizException("CODECENTER-THEME-6005",
                "Domain name length > 50: " + name.length());
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new BizException("CODECENTER-THEME-6006",
                    "Domain name must contain only Chinese/letter/digit/-/_");
        }
    }
}
