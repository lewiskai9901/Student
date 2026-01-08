package com.school.management.dto.rating;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 计算评级请求DTO
 *
 * @author system
 * @since 3.2.0
 */
@Data
public class CalculateRatingDTO {

    /**
     * 规则ID（可选，不指定则计算所有启用的规则）
     */
    private Long ruleId;

    /**
     * 检查记录ID（DAILY类型时使用）
     */
    private Long checkRecordId;

    /**
     * 统计周期开始（SUMMARY类型时使用，不指定则默认为检查计划开始日期）
     */
    private LocalDate periodStart;

    /**
     * 统计周期结束（SUMMARY类型时使用，不指定则默认为检查计划结束日期或当前日期）
     */
    private LocalDate periodEnd;

    /**
     * 是否强制重新计算（覆盖已有结果）
     */
    private Boolean forceRecalculate = false;
}
