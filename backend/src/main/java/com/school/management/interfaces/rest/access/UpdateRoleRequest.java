package com.school.management.interfaces.rest.access;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating an existing role.
 */
@Data
public class UpdateRoleRequest {

    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String roleName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /** 启用/禁用角色 (null=不改)。与编辑表单的状态开关对应, 折叠进 PUT 而非单独 enable/disable 端点。 */
    private Boolean isEnabled;
}
