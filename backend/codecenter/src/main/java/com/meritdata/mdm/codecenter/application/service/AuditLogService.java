package com.meritdata.mdm.codecenter.application.service;

import com.meritdata.mdm.codecenter.common.util.IdUtil;
import com.meritdata.mdm.codecenter.domain.entity.ModelAuditLog;
import com.meritdata.mdm.codecenter.domain.repository.ModelAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审计日志服务
 *
 * 三员 / 多租户 / 安全合规必备
 * 记录所有模型/规则/码段的关键操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final ModelAuditLogRepository auditLogRepository;

    @Transactional
    public void record(String operatorId, String operatorName, String operationType,
                       String targetId, String targetType,
                       String beforeState, String afterState, String diffSnapshot) {
        try {
            ModelAuditLog log = ModelAuditLog.builder()
                    .logId(IdUtil.simpleId())
                    .operatorId(operatorId == null ? "system" : operatorId)
                    .operatorName(operatorName)
                    .operationType(operationType)
                    .targetId(targetId)
                    .targetType(targetType)
                    .beforeState(beforeState)
                    .afterState(afterState)
                    .diffSnapshot(diffSnapshot)
                    .operatorIp(org.slf4j.MDC.get("clientIp"))
                    .operatedAt(LocalDateTime.now())
                    .tenantId(org.slf4j.MDC.get("tenantId"))
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            // 审计日志失败不影响主业务
            log.warn("Audit log save failed: operator={}, op={}, target={}/{}",
                    operatorId, operationType, targetType, targetId, e);
        }
    }
}
