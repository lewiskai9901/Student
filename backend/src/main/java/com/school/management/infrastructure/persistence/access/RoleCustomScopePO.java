package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色自定义数据范围持久化对象
 */
@Data
@TableName("role_custom_scope")
public class RoleCustomScopePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 模块代码
     */
    @TableField("module_code")
    private String moduleCode;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
