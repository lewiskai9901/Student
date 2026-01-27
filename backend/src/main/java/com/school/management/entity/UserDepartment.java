package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户部门关联实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@TableName("user_departments")
public class UserDepartment {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 是否主部门: 0-否, 1-是
     */
    private Integer isPrimary;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 组织单元名称 (关联查询获取)
     */
    @TableField(exist = false)
    private String orgUnitName;

    /**
     * 用户名称 (关联查询获取)
     */
    @TableField(exist = false)
    private String userName;
}
