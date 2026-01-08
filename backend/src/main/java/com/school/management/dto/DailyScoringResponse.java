package com.school.management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 日常检查打分响应
 *
 * @author system
 * @since 1.0.6
 */
@Data
public class DailyScoringResponse {

    /**
     * 检查ID
     */
    private Long checkId;

    /**
     * 检查名称
     */
    private String checkName;

    /**
     * 检查日期
     */
    private LocalDate checkDate;

    /**
     * 检查员ID
     */
    private Long checkerId;

    /**
     * 检查员姓名
     */
    private String checkerName;

    /**
     * 总扣分
     */
    private BigDecimal totalDeductScore;

    /**
     * 明细数量
     */
    private Integer detailCount;

    /**
     * 申诉状态 (0=无申诉, 1=有待处理申诉, 2=申诉已处理)
     */
    private Integer appealStatus;

    /**
     * 申诉数量
     */
    private Integer appealCount;

    /**
     * 扣分明细列表
     */
    private List<ScoringDetailResponse> details;
}
