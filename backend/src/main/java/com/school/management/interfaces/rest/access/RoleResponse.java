package com.school.management.interfaces.rest.access;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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

    /** 行业包: CORE / EDU / ... null 表示 admin 自定义. @deprecated 用 origin 字段 */
    @Deprecated
    private String industry;

    /** 来源插件全限定类名,null 表示非插件声明. @deprecated 用 origin 字段 */
    @Deprecated
    private String pluginClass;

    /**
     * 统一来源字段 (Phase 1 引入,将替代 industry + pluginClass).
     * 格式:
     *   "PLUGIN:CORE@1.0.0"   插件主流声明
     *   "TENANT:CUSTOM#<id>"  租户自定义
     *   null                  未归属 (脏数据)
     */
    private String origin;

    /**
     * 插件级启用状态 (两状态模型, 与 isEnabled 区分).
     * true = 所属插件处于启用状态, false = 插件被禁 (级联软失效).
     * 前端列表应对 pluginEnabled=false 的行做灰显 + 徽章提示.
     */
    private Boolean pluginEnabled;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private Set<Long> permissionIds;

    private LocalDateTime createdAt;
}
