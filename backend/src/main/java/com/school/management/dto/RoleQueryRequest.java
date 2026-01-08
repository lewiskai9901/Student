package com.school.management.dto;

import lombok.Data;

/**
 * 角色查询请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class RoleQueryRequest {

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 页大小
     */
    private Integer pageSize = 10;
}