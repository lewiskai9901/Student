package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for roles.
 * Maps to the existing 'roles' table.
 */
@Data
@TableName("roles")
public class RolePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色类型（自由字符串，如 SUPER_ADMIN, CUSTOM 等）
     */
    private String roleType;

    /**
     * 角色描述
     */
    @TableField("role_desc")
    private String roleDesc;

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

    /** 来源插件全限定类名(由 RolePresetRegistrar 写入). @deprecated 用 origin */
    @Deprecated
    private String pluginClass;

    /** 统一来源字段: "PLUGIN:CORE@1.0.0" / "TENANT:CUSTOM#1" */
    private String origin;

    /**
     * 插件级启用状态 (0=所属插件被禁)
     * 两状态模型: 实际生效 = status AND plugin_enabled
     * 该字段由 PluginLifecycleService 级联维护, 不由管理员直接 update
     * updateStrategy=NEVER 防止 updateById 覆盖为 null
     */
    @TableField(value = "plugin_enabled", updateStrategy = FieldStrategy.NEVER)
    private Integer pluginEnabled;
}
