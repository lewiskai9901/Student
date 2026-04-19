package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for permissions.
 * Maps to the existing 'permissions' table.
 */
@Data
@TableName("permissions")
public class PermissionPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限描述
     */
    private String permissionDesc;

    /**
     * 资源类型: 1菜单 2按钮 3接口
     */
    private Integer resourceType;

    /**
     * 权限作用域: PUBLIC | SELF | MANAGEMENT
     */
    private String permissionScope;

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
    private Integer sortOrder;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    private Long tenantId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    /** 行业包: CORE / EDU / MED / ... @deprecated 用 origin */
    @Deprecated
    private String industry;

    /** 来源插件全限定类名(由 PermissionRegistrar 写入). @deprecated 用 origin */
    @Deprecated
    private String pluginClass;

    /** 统一来源字段: "PLUGIN:CORE@1.0.0" / "TENANT:CUSTOM#1" */
    private String origin;
}
