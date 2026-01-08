package com.school.management.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 用户数据范围DTO
 *
 * @author system
 * @version 3.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDataScopeDTO {

    /**
     * ID
     */
    private Long id;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户真实姓名
     */
    private String realName;

    /**
     * 范围类型: DEPARTMENT-部门, GRADE-年级, CLASS-班级
     */
    @NotBlank(message = "范围类型不能为空")
    private String scopeType;

    /**
     * 范围类型名称
     */
    private String scopeTypeName;

    /**
     * 范围ID
     */
    @NotNull(message = "范围ID不能为空")
    private Long scopeId;

    /**
     * 范围名称
     */
    private String scopeName;

    /**
     * 是否包含下级: 1-是, 0-否
     */
    private Integer includeChildren;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人姓名
     */
    private String createdByName;
}
