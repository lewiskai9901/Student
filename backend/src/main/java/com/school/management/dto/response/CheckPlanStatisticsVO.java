package com.school.management.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 检查计划统计VO
 *
 * @author system
 * @since 3.0.0
 */
@Data
public class CheckPlanStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总计划数
     */
    private Integer totalPlans;

    /**
     * 草稿数
     */
    private Integer draftCount;

    /**
     * 进行中数
     */
    private Integer inProgressCount;

    /**
     * 已结束数
     */
    private Integer finishedCount;

    /**
     * 已归档数
     */
    private Integer archivedCount;

    /**
     * 总检查次数
     */
    private Integer totalChecks;

    /**
     * 总检查记录数
     */
    private Integer totalRecords;

    /**
     * 总扣分
     */
    private BigDecimal totalDeductionScore;
}
