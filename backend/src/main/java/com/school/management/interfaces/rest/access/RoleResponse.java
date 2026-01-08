package com.school.management.interfaces.rest.access;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.RoleType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for role information.
 */
@Data
public class RoleResponse {

    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private RoleType roleType;

    private Integer level;

    private Boolean isSystem;

    private Boolean isEnabled;

    private DataScope dataScope;

    private Set<Long> permissionIds;

    private LocalDateTime createdAt;
}
