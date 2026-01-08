package com.school.management.casbin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户范围分配实体
 *
 * @author system
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_scope_assignments")
public class UserScopeAssignment {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 范围表达式
     */
    private String scopeExpression;

    /**
     * 范围类型
     */
    private String scopeType;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 分配人ID
     */
    private Long assignedBy;

    /**
     * 分配时间
     */
    private LocalDateTime assignedAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ========== 关联字段 ==========

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
     * 分配人姓名
     */
    @TableField(exist = false)
    private String assignedByName;
}
