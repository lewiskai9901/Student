package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for role-permission mappings.
 */
@Data
@TableName("role_permissions")
public class RolePermissionPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long roleId;

    private Long permissionId;

    private LocalDateTime createdAt;
}
