package com.school.management.dto.record;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 检查记录查询DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class CheckRecordQueryDTO {

    /**
     * 检查名称（模糊匹配）
     */
    private String checkName;

    /**
     * 检查日期开始
     */
    private LocalDate checkDateStart;

    /**
     * 检查日期结束
     */
    private LocalDate checkDateEnd;

    /**
     * 状态：1=已发布 2=已归档
     */
    private Integer status;

    /**
     * 检查类型：1=日常检查 2=专项检查
     */
    private Integer checkType;

    /**
     * 检查计划ID
     */
    private Long planId;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    // ========== 数据权限过滤字段 ==========

    /**
     * 班级ID列表(数据权限过滤用 - 查询包含这些班级的检查记录)
     */
    private List<Long> classIds;
}
