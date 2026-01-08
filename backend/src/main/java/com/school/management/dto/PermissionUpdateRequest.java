package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 权限更新请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class PermissionUpdateRequest {

    /**
     * 权限ID
     */
    @NotNull(message = "权限ID不能为空")
    private Long id;

    /**
     * 权限名称
     */
    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    /**
     * 权限编码
     */
    @NotBlank(message = "权限编码不能为空")
    private String permissionCode;

    /**
     * 权限描述
     */
    private String permissionDesc;

    /**
     * 资源类型: 1菜单 2按钮 3接口
     */
    @NotNull(message = "资源类型不能为空")
    private Integer resourceType;

    /**
     * 父权限ID
     */
    private Long parentId;

    /**
     * 前端路由路径
     */
    private String path;

    /**
     * 前端组件路径
     */
    private String component;

    /**
     * 图标
     */
    private String icon;

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
}