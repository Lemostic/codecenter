package com.meritdata.mdm.codecenter.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public BizException(String errorCode, String message) {
        this(errorCode, message, new Object[0]);
    }

    public BizException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args == null ? new Object[0] : args;
    }

    public BizException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public static BizException ruleNotFound(String ruleId) {
        return new BizException("CODECENTER-RULE-1001", "Code rule not found: " + ruleId);
    }
    public static BizException ruleStatusInvalid(String current) {
        return new BizException("CODECENTER-RULE-1002",
                "Rule status does not allow this operation: " + current);
    }
    public static BizException segmentNotFound(String segmentId) {
        return new BizException("CODECENTER-SEG-2001", "Code segment not found: " + segmentId);
    }
    public static BizException segmentArchived(String segmentCode) {
        return new BizException("CODECENTER-SEG-2002", "Code segment archived: " + segmentCode);
    }
    public static BizException codeAlreadyExists(String code) {
        return new BizException("CODECENTER-CODE-3002", "Code already exists: " + code);
    }
    public static BizException modelNotFound(String modelId) {
        return new BizException("CODECENTER-MODEL-4001", "Model not found: " + modelId);
    }
    public static BizException modelStatusInvalid(String current) {
        return new BizException("CODECENTER-MODEL-4002",
                "Model status does not allow this operation: " + current);
    }
    public static BizException paramInvalid(String field) {
        return new BizException("CODECENTER-VALID-5001", "Parameter validation failed: " + field);
    }
    public static BizException tableAlreadyExists(String tableName) {
        return new BizException("CODECENTER-MODEL-4003", "Physical table already exists: " + tableName);
    }
}
