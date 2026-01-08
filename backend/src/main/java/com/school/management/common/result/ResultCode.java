package com.school.management.common.result;

/**
 * 响应状态码枚举
 *
 * @author system
 * @since 1.0.0
 */
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误
    ERROR(400, "操作失败"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),
    VALIDATION_ERROR(422, "参数验证失败"),
    VALIDATE_ERROR(422, "参数验证失败"),  // 别名，兼容历史代码
    PARAM_ERROR(422, "参数错误"),  // 用于更具体的参数错误
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // 服务器错误
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 业务错误码
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已被禁用"),
    USERNAME_EXISTS(1003, "用户名已存在"),
    PASSWORD_ERROR(1004, "密码错误"),
    TOKEN_INVALID(1005, "令牌无效"),
    TOKEN_EXPIRED(1006, "令牌已过期"),
    REFRESH_TOKEN_INVALID(1007, "刷新令牌无效"),
    LOGIN_FAILED(1008, "登录失败"),
    LOGOUT_FAILED(1009, "退出登录失败"),
    PERMISSION_DENIED(1010, "权限不足"),

    // 数据相关错误
    DATA_NOT_FOUND(2001, "数据不存在"),
    DATA_EXISTS(2002, "数据已存在"),
    DATA_INVALID(2003, "数据无效"),
    DATA_OPERATION_ERROR(2004, "数据操作失败"),
    DATA_ERROR(2005, "数据错误"),

    // 文件相关错误
    FILE_NOT_FOUND(3001, "文件不存在"),
    FILE_UPLOAD_ERROR(3002, "文件上传失败"),
    FILE_TYPE_ERROR(3003, "文件类型不支持"),
    FILE_SIZE_ERROR(3004, "文件大小超限"),

    // 学生相关错误
    STUDENT_NOT_FOUND(4001, "学生不存在"),
    STUDENT_NO_EXISTS(4002, "学号已存在"),
    CLASS_NOT_FOUND(4003, "班级不存在"),
    DORMITORY_NOT_FOUND(4004, "宿舍不存在"),
    DORMITORY_FULL(4005, "宿舍已满"),

    // 操作相关错误
    OPERATION_NOT_ALLOWED(5001, "操作不被允许");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}