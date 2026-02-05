package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织类型持久化对象
 */
@Data
@TableName("org_types")
public class OrgTypePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("type_code")
    private String typeCode;

    @TableField("type_name")
    private String typeName;

    @TableField("parent_type_code")
    private String parentTypeCode;

    @TableField("level_order")
    private Integer levelOrder;

    @TableField("icon")
    private String icon;

    @TableField("color")
    private String color;

    @TableField("description")
    private String description;

    // 特性配置
    @TableField("can_have_classes")
    private Boolean canHaveClasses;

    @TableField("can_have_students")
    private Boolean canHaveStudents;

    @TableField("can_be_inspected")
    private Boolean canBeInspected;

    @TableField("can_have_leader")
    private Boolean canHaveLeader;

    // 系统标识
    @TableField("is_system")
    private Boolean isSystem;

    @TableField("is_enabled")
    private Boolean isEnabled;

    @TableField("sort_order")
    private Integer sortOrder;

    // 审计字段
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}
