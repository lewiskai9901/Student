package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for departments.
 * Maps to the existing 'departments' table.
 */
@Data
@TableName("departments")
public class OrgUnitPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 部门描述
     */
    private String deptDesc;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 部门层级
     */
    private Integer deptLevel;

    /**
     * 部门路径
     */
    private String deptPath;

    /**
     * 部门负责人ID
     */
    private Long leaderId;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 地址
     */
    private String address;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    /**
     * 组织类别: academic, functional, administrative
     */
    private String unitCategory;

    /**
     * 组织类型: school, department, major, grade, class, student_affairs, etc.
     */
    private String unitType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
