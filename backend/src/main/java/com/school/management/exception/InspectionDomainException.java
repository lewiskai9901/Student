package com.school.management.exception;

/**
 * 检查领域异常
 * 用于检查模板、会话、记录、申诉等业务规则违反
 */
public class InspectionDomainException extends BusinessException {

    public InspectionDomainException(String message) {
        super(ErrorCode.OPERATION_NOT_ALLOWED, message);
    }

    public InspectionDomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InspectionDomainException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static InspectionDomainException templateNotFound(Long templateId) {
        return new InspectionDomainException(ErrorCode.DATA_NOT_FOUND, "检查模板不存在: " + templateId);
    }

    public static InspectionDomainException sessionNotFound(Long sessionId) {
        return new InspectionDomainException(ErrorCode.DATA_NOT_FOUND, "检查会话不存在: " + sessionId);
    }

    public static InspectionDomainException recordNotFound(Long recordId) {
        return new InspectionDomainException(ErrorCode.CHECK_RECORD_NOT_FOUND, "检查记录不存在: " + recordId);
    }

    public static InspectionDomainException invalidStateTransition(String from, String to) {
        return new InspectionDomainException("非法状态转换: " + from + " → " + to);
    }

    public static InspectionDomainException duplicateTemplateCode(String code) {
        return new InspectionDomainException(ErrorCode.DATA_ALREADY_EXISTS, "模板编码已存在: " + code);
    }
}
