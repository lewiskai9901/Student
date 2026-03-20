package com.school.management.interfaces.rest.access;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.school.management.domain.access.model.DataScope;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for role information.
 */
@Data
public class RoleResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private String roleType;

    private Integer level;

    private Boolean isSystem;

    private Boolean isEnabled;

    private DataScope dataScope;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private Set<Long> permissionIds;

    private LocalDateTime createdAt;
}
