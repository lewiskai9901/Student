package com.school.management.dto.request;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 查询检查计划请求DTO
 *
 * @author system
 * @since 3.0.0
 */
@Data
public class CheckPlanQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 计划名称(模糊查询)
     */
    private String planName;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 状态(0=草稿,1=进行中,2=已结束,3=已归档)
     */
    private Integer status;

    /**
     * 开始日期范围-起始
     */
    private LocalDate startDateFrom;

    /**
     * 开始日期范围-结束
     */
    private LocalDate startDateTo;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
