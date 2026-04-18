package com.school.management.interfaces.rest.access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a new role.
 */
@Data
public class CreateRoleRequest {

    @NotBlank(message = "角色编码必填")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,49}$",
            message = "角色编码必须以大写字母开头，仅允许大写字母/数字/下划线，长度 3-50")
    private String roleCode;

    @NotBlank(message = "角色名称必填")
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String roleName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String roleType = "CUSTOM";
}
