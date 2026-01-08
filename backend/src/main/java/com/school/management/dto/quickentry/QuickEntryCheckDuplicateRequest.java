package com.school.management.dto.quickentry;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 快捷录入 - 检查重复请求DTO
 *
 * @author system
 * @since 1.0.7
 */
@Data
public class QuickEntryCheckDuplicateRequest {

    /**
     * 扣分项ID
     */
    @NotNull(message = "扣分项ID不能为空")
    private Long deductionItemId;

    /**
     * 学生ID
     */
    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    /**
     * 检查轮次（可选，不传则检查所有轮次）
     */
    private Integer checkRound;
}
