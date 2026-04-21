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

    /** 行业包: CORE / EDU / ... @deprecated 用 origin */
    @Deprecated
    private String industry;

    /** 来源插件全限定类名. @deprecated 用 origin */
    @Deprecated
    private String pluginClass;

    /** 统一来源: "PLUGIN:CORE@1.0.0" / "TENANT:CUSTOM#<id>" */
    private String origin;

    /**
     * 插件级启用状态 (两状态模型).
     * true = 所属插件启用, false = 被级联软失效 (前端应灰显).
     */
    private Boolean pluginEnabled;

    private LocalDateTime createdAt;

    // Children for tree structure
    private List<PermissionResponse> children;
}
