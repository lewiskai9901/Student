package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * v3 角色数据权限持久化对象 (role_data_scopes 表)
 * 替代 v2 的 role_data_permissions_v5 + role_data_scope_items 组合
 */
@Data
@TableName("role_data_scopes")
public class RoleDataScopePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;
    private String resourceCode;
    private String scopeType;

    /** CUSTOM 时的自定义组织 ID 列表(JSON 数组) */
    private String customOrgUnitIds;

    private Integer priority;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
