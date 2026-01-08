package com.school.management.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 专业查询请求DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MajorQueryRequest extends BaseQueryRequest {

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 专业编码
     */
    private String majorCode;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 状态
     */
    private Integer status;
}
