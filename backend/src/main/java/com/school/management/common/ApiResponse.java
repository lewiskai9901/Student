package com.school.management.common;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一API响应格式
 *
 * @author system
 * @since 1.0.0
 * @deprecated 使用 {@link com.school.management.common.result.Result} 替代
 */
@Deprecated(since = "2.0.0", forRemoval = true)
@Data
public class ApiResponse<T> {

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 请求ID
     */
    private String requestId;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(Integer code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    public static <T> ApiResponse<T> error() {
        return new ApiResponse<>(500, "系统异常", null);
    }

    public static <T> ApiResponse<T> unauthorized() {
        return new ApiResponse<>(401, "未授权访问", null);
    }

    public static <T> ApiResponse<T> forbidden() {
        return new ApiResponse<>(403, "禁止访问", null);
    }

    public static <T> ApiResponse<T> notFound() {
        return new ApiResponse<>(404, "资源未找到", null);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }
}