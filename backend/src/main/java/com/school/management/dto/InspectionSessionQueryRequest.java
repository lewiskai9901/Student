package com.school.management.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 检查批次查询请求DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class InspectionSessionQueryRequest {

    /**
     * 检查日期开始
     */
    private LocalDate startDate;

    /**
     * 检查日期结束
     */
    private LocalDate endDate;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 检查人ID
     */
    private Long inspectorId;

    /**
     * 状态:1草稿 2待审核 3已发布
     */
    private Integer status;

    /**
     * 检查批次编号
     */
    private String sessionCode;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
