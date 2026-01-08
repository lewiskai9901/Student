package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 部门创建请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DepartmentCreateRequest {

    /**
     * 部门名称
     */
    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    /**
     * 部门编码
     */
    @NotBlank(message = "部门编码不能为空")
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
    @NotNull(message = "排序不能为空")
    private Integer sortOrder;

    /**
     * 状态: 1启用 0禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}