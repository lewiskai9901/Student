package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for user-role mappings with scope support.
 */
@Data
@TableName("user_roles")
public class UserRolePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long roleId;

    private Long tenantId;

    private String scopeType;

    private Long scopeId;

    private LocalDateTime assignedAt;

    private Long assignedBy;

    private LocalDateTime expiresAt;

    private Boolean isActive;
}
