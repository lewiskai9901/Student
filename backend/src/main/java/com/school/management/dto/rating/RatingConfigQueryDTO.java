package com.school.management.dto.rating;

import lombok.Data;

/**
 * 评级配置查询 DTO
 *
 * @author System
 * @since 4.4.0
 */
@Data
public class RatingConfigQueryDTO {

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 评级名称（模糊查询）
     */
    private String ratingName;

    /**
     * 评级周期类型：DAILY/WEEKLY/MONTHLY
     */
    private String ratingType;

    /**
     * 是否启用：0否 1是
     */
    private Integer enabled;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
