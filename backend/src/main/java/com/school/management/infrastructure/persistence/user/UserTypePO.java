package com.school.management.infrastructure.persistence.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户类型持久化对象
 */
@Data
@TableName("user_types")
public class UserTypePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 类型编码
     */
    private String typeCode;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 父类型编码
     */
    private String parentTypeCode;

    /**
     * 层级顺序
     */
    private Integer levelOrder;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否可登录系统
     */
    private Boolean canLogin;

    /**
     * 是否可作为检查员
     */
    private Boolean canBeInspector;

    /**
     * 是否可被检查
     */
    private Boolean canBeInspected;

    /**
     * 是否可管理组织
     */
    private Boolean canManageOrg;

    /**
     * 是否可查看报表
     */
    private Boolean canViewReports;

    /**
     * 是否需要班级归属
     */
    private Boolean requiresClass;

    /**
     * 是否需要宿舍归属
     */
    private Boolean requiresDormitory;

    /**
     * 默认角色编码（逗号分隔）
     */
    private String defaultRoleCodes;

    /**
     * 是否系统预置
     */
    private Boolean isSystem;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
