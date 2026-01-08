package com.school.management.dto.rating;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 评级审核请求DTO
 */
@Data
public class RatingApprovalDTO {

    /**
     * 评级结果ID列表（批量审核）
     */
    @NotNull(message = "评级结果ID不能为空")
    private List<Long> resultIds;

    /**
     * 审核操作: APPROVE REJECT
     */
    @NotNull(message = "审核操作不能为空")
    private String action;

    /**
     * 审核备注
     */
    private String remark;
}
