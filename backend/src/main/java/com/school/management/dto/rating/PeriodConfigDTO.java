package com.school.management.dto.rating;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 周期配置 DTO
 *
 * @author System
 * @since 4.4.0
 */
@Data
public class PeriodConfigDTO {

    /**
     * 检查计划ID
     */
    @NotNull(message = "检查计划ID不能为空")
    private Long checkPlanId;

    /**
     * 周起始日：1-7（1=周一，7=周日）
     */
    @NotNull(message = "周起始日不能为空")
    @Min(value = 1, message = "周起始日必须在1-7之间")
    @Max(value = 7, message = "周起始日必须在1-7之间")
    private Integer weekStartDay;
}
