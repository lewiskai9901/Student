package com.school.management.interfaces.rest.access;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.school.management.domain.access.model.PermissionScope;
import com.school.management.domain.access.model.PermissionType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for permission information.
 */
@Data
public class PermissionResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String permissionCode;

    private String permissionName;

    private String description;

    private String resource;

    private String action;

    private PermissionType type;

    private PermissionScope scope;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private Integer sortOrder;

    private Boolean isEnabled;

    private LocalDateTime createdAt;

    // Children for tree structure
    private List<PermissionResponse> children;
}
