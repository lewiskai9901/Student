package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色创建请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class RoleCreateRequest {

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /**
     * 角色编码
     */
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    /**
     * 角色描述
     */
    private String roleDesc;

    /**
     * 排序号
     */
    @NotNull(message = "排序号不能为空")
    private Integer sortOrder;

    /**
     * 状态: 1启用 0禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 权限ID列表
     */
    private List<Long> permissionIds;
}