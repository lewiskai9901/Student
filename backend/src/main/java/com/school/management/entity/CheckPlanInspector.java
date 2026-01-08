package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检查计划打分人员实体
 */
@Data
@TableName("check_plan_inspectors")
public class CheckPlanInspector {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 检查计划ID
     */
    private Long planId;

    /**
     * 打分人员用户ID
     */
    private Long userId;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    // ========== 非数据库字段 ==========

    /**
     * 用户姓名（关联查询）
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 用户账号（关联查询）
     */
    @TableField(exist = false)
    private String username;

    /**
     * 部门名称（关联查询）
     */
    @TableField(exist = false)
    private String departmentName;

    // 手动添加getter方法，以避免Lombok注解处理器问题
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}
