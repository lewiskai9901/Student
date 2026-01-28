package com.school.management.exception;

/**
 * 组织管理领域异常
 * 用于组织单元、班级、学生等业务规则违反
 */
public class OrganizationDomainException extends BusinessException {

    public OrganizationDomainException(String message) {
        super(ErrorCode.OPERATION_NOT_ALLOWED, message);
    }

    public OrganizationDomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    public OrganizationDomainException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static OrganizationDomainException orgUnitNotFound(Long orgUnitId) {
        return new OrganizationDomainException(ErrorCode.DATA_NOT_FOUND, "组织单元不存在: " + orgUnitId);
    }

    public static OrganizationDomainException classNotFound(Long classId) {
        return new OrganizationDomainException(ErrorCode.CLASS_NOT_FOUND);
    }

    public static OrganizationDomainException studentNotFound(Long studentId) {
        return new OrganizationDomainException(ErrorCode.DATA_NOT_FOUND, "学生不存在: " + studentId);
    }

    public static OrganizationDomainException duplicateCode(String code) {
        return new OrganizationDomainException(ErrorCode.DATA_ALREADY_EXISTS, "编码已存在: " + code);
    }
}
