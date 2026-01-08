package com.school.management.dto.rating;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评级修改请求DTO
 */
@Data
public class RatingModifyDTO {

    /**
     * 评级结果ID
     */
    @NotNull(message = "评级结果ID不能为空")
    private Long resultId;

    /**
     * 新的等级ID
     */
    @NotNull(message = "新等级ID不能为空")
    private Long newLevelId;

    /**
     * 修改原因
     */
    private String reason;
}
