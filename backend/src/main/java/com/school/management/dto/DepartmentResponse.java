package com.school.management.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DepartmentResponse {

    /**
     * 部门ID
     */
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
     * 父部门名称
     */
    private String parentName;

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
     * 部门负责人姓名
     */
    private String leaderName;

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
     * 状态名称
     */
    private String statusName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 子部门列表
     */
    private List<DepartmentResponse> children;
}