package com.school.management.application.access;

import com.school.management.domain.access.model.PermissionType;
import lombok.Builder;
import lombok.Data;

/**
 * Command to create a new permission.
 */
@Data
@Builder
public class CreatePermissionCommand {
    private String permissionCode;
    private String permissionName;
    private String description;
    private String resource;
    private String action;
    private PermissionType type;
    private Long parentId;
    private Integer sortOrder;
}
