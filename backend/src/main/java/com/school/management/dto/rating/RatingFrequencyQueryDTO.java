package com.school.management.dto.rating;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 评级频次查询DTO
 */
@Data
public class RatingFrequencyQueryDTO {

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 规则ID（可选，不指定则查询所有规则）
     */
    private Long ruleId;

    /**
     * 等级ID列表（可选，筛选特定等级）
     */
    private List<Long> levelIds;

    /**
     * 班级ID列表（可选）
     */
    private List<Long> classIds;

    /**
     * 年级ID列表（可选）
     */
    private List<Long> gradeIds;

    /**
     * 周期类型: WEEK MONTH QUARTER SEMESTER YEAR CUSTOM
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
     * 排序字段: frequency, className, gradeName
     */
    private String sortBy = "frequency";

    /**
     * 排序方向: ASC DESC
     */
    private String sortOrder = "DESC";

    /**
     * 返回数量限制（TOP N）
     */
    private Integer limit;
}
