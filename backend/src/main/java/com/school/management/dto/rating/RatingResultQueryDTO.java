package com.school.management.dto.rating;

import lombok.Data;
import java.time.LocalDate;

/**
 * 评级结果查询 DTO
 *
 * @author System
 * @since 4.4.0
 */
@Data
public class RatingResultQueryDTO {

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 评级配置ID
     */
    private Long ratingConfigId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 评级周期类型：DAILY/WEEKLY/MONTHLY
     */
    private String periodType;

    /**
     * 周期开始日期（范围查询-起始）
     */
    private LocalDate periodStartFrom;

    /**
     * 周期开始日期（范围查询-结束）
     */
    private LocalDate periodStartTo;

    /**
     * 是否获得该评级：0否 1是
     */
    private Integer awarded;

    /**
     * 状态：DRAFT/PENDING_APPROVAL/PUBLISHED
     */
    private String status;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
