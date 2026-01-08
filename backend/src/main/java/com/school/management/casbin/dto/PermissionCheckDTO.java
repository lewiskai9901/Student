package com.school.management.casbin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 权限检查请求DTO
 *
 * @author system
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckDTO {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 范围表达式
     */
    private String scope;

    /**
     * 资源
     */
    @NotBlank(message = "资源不能为空")
    private String resource;

    /**
     * 操作
     */
    @NotBlank(message = "操作不能为空")
    private String action;

    /**
     * 检查结果
     */
    private Boolean allowed;
}
