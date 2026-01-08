package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for user-role mappings.
 */
@Data
@TableName("user_roles")
public class UserRolePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long roleId;

    private Long orgUnitId;

    private LocalDateTime assignedAt;

    private Long assignedBy;

    private LocalDateTime expiresAt;

    private Boolean isActive;
}
