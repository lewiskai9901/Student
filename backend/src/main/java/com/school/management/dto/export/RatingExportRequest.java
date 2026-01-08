package com.school.management.dto.export;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 评级数据导出请求
 */
@Data
public class RatingExportRequest {

    /**
     * 导出类型: RATING_RESULT(评级结果), FREQUENCY(频次统计), STATISTICS(统计分析)
     */
    private String exportType;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 评级规则ID
     */
    private Long ruleId;

    /**
     * 等级ID列表（筛选特定等级）
     */
    private List<Long> levelIds;

    /**
     * 班级ID列表
     */
    private List<Long> classIds;

    /**
     * 年级ID列表
     */
    private List<Long> gradeIds;

    /**
     * 周期类型
     */
    private String periodType;

    /**
     * 周期开始日期
     */
    private LocalDate periodStart;

    /**
     * 周期结束日期
     */
    private LocalDate periodEnd;

    /**
     * 审核状态筛选
     */
    private Integer approvalStatus;

    /**
     * 发布状态筛选
     */
    private Integer publishStatus;

    /**
     * 导出格式: EXCEL, PDF
     */
    private String format = "EXCEL";
}
