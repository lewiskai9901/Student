package com.school.management.exception;

/**
 * 权限管理领域异常
 * 用于角色、权限、用户访问控制等业务规则违反
 */
public class AccessDomainException extends BusinessException {

    public AccessDomainException(String message) {
        super(ErrorCode.PERMISSION_DENIED, message);
    }

    public AccessDomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AccessDomainException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static AccessDomainException roleNotFound(Long roleId) {
        return new AccessDomainException(ErrorCode.ROLE_NOT_FOUND);
    }

    public static AccessDomainException userNotFound(Long userId) {
        return new AccessDomainException(ErrorCode.USER_NOT_FOUND);
    }

    public static AccessDomainException permissionDenied(String action) {
        return new AccessDomainException(ErrorCode.PERMISSION_DENIED, "权限不足: " + action);
    }

    public static AccessDomainException duplicateRoleCode(String code) {
        return new AccessDomainException(ErrorCode.DATA_ALREADY_EXISTS, "角色编码已存在: " + code);
    }
}
