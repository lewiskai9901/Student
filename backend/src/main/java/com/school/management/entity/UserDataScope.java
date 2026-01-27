package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.school.management.casbin.enums.ScopeType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 用户数据范围实体
 *
 * 用于定义用户可以访问的数据范围
 * 核心设计：角色（功能权限）与数据范围（数据权限）正交分离
 *
 * @author system
 * @version 3.0.0
 * @since 2024-01-01
 * @deprecated 用户级数据范围已废弃，请使用角色级数据权限配置
 *             迁移说明：数据权限现在通过 role_data_permissions 和 role_custom_scope 表在角色级别配置
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("user_data_scopes")
public class UserDataScope extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 范围类型: DEPARTMENT-部门, GRADE-年级, CLASS-班级
     */
    private String scopeType;

    /**
     * 范围ID（部门ID/年级ID/班级ID）
     */
    private Long scopeId;

    /**
     * 范围名称（冗余字段，方便显示）
     */
    private String scopeName;

    /**
     * 是否包含下级: 1-是, 0-否
     * 部门包含子部门和班级，年级包含班级
     */
    private Integer includeChildren;

    // ========== 关联字段（非数据库字段） ==========

    /**
     * 用户信息
     */
    @TableField(exist = false)
    private User user;

    /**
     * 用户名
     */
    @TableField(exist = false)
    private String username;

    /**
     * 用户真实姓名
     */
    @TableField(exist = false)
    private String realName;

    /**
     * 获取范围类型枚举
     */
    public ScopeType getScopeTypeEnum() {
        return ScopeType.fromCode(this.scopeType);
    }

    /**
     * 设置范围类型枚举
     */
    public void setScopeTypeEnum(ScopeType scopeType) {
        this.scopeType = scopeType != null ? scopeType.getCode() : null;
    }

    /**
     * 是否包含下级（便捷方法）
     */
    public boolean hasIncludeChildren() {
        return this.includeChildren != null && this.includeChildren == 1;
    }
}
