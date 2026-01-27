package com.school.management.application.access;

import com.school.management.casbin.model.DataScope;
import com.school.management.domain.access.model.RoleType;
import lombok.Builder;
import lombok.Data;

/**
 * Command to create a new role.
 */
@Data
@Builder
public class CreateRoleCommand {
    private String roleCode;
    private String roleName;
    private String description;
    private RoleType roleType;
    private DataScope dataScope;
    private Long createdBy;
}
