package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 部门实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("departments")
public class Department extends BaseEntity {

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

    // 关联字段（非数据库字段）

    /**
     * 部门负责人信息
     */
    @TableField(exist = false)
    private User leader;

    /**
     * 子部门列表
     */
    @TableField(exist = false)
    private List<Department> children;
}