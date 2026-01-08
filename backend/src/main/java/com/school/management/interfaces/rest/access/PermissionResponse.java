package com.school.management.interfaces.rest.access;

import com.school.management.domain.access.model.PermissionType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for permission information.
 */
@Data
public class PermissionResponse {

    private Long id;

    private String permissionCode;

    private String permissionName;

    private String description;

    private String resource;

    private String action;

    private PermissionType type;

    private Long parentId;

    private Integer sortOrder;

    private Boolean isEnabled;

    private LocalDateTime createdAt;
}
