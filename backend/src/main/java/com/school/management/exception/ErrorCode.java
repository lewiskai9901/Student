package com.school.management.exception;

import lombok.Getter;

/**
 * 业务错误码枚举
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Getter
public enum ErrorCode {

    // 通用错误 (1000-1999)
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限访问"),
    
    // 业务错误 (2000-2999)
    DATA_NOT_FOUND(2001, "数据不存在"),
    DATA_ALREADY_EXISTS(2002, "数据已存在"),
    DATA_IN_USE(2003, "数据正在使用中"),
    OPERATION_NOT_ALLOWED(2004, "不允许的操作"),
    
    // 用户相关 (3000-3099)
    USER_NOT_FOUND(3001, "用户不存在"),
    USER_ALREADY_EXISTS(3002, "用户已存在"),
    PASSWORD_ERROR(3003, "密码错误"),
    USER_DISABLED(3004, "用户已被禁用"),
    
    // 检查记录相关 (3100-3199)
    CHECK_RECORD_NOT_FOUND(3101, "检查记录不存在"),
    CHECK_RECORD_ALREADY_PUBLISHED(3102, "检查记录已发布"),
    CHECK_RECORD_ALREADY_CONVERTED(3103, "日常检查已转换为检查记录"),
    DAILY_CHECK_NOT_FOUND(3104, "日常检查不存在"),
    NO_DEDUCTION_DETAILS(3105, "该日常检查没有扣分明细数据"),
    
    // 申诉相关 (3200-3299)
    APPEAL_NOT_FOUND(3201, "申诉不存在"),
    APPEAL_ALREADY_REVIEWED(3202, "申诉已审核"),
    APPEAL_NOT_ALLOWED(3203, "不允许申诉"),
    APPEAL_TIMEOUT(3204, "申诉已超时"),
    
    // 班级相关 (3300-3399)
    CLASS_NOT_FOUND(3301, "班级不存在"),
    CLASS_ALREADY_EXISTS(3302, "班级已存在"),
    NOT_CLASS_TEACHER(3303, "您不是该班级的班主任"),
    
    // 权限相关 (3400-3499)
    PERMISSION_DENIED(3401, "权限不足"),
    ROLE_NOT_FOUND(3402, "角色不存在"),
    
    // 系统错误 (9000-9999)
    SYSTEM_ERROR(9001, "系统错误"),
    DATABASE_ERROR(9002, "数据库错误"),
    NETWORK_ERROR(9003, "网络错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
