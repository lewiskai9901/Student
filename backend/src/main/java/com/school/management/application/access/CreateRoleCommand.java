package com.school.management.application.access;

import com.school.management.domain.access.model.DataScope;
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
    private String roleType;
    private DataScope dataScope;
    private Long createdBy;
}
