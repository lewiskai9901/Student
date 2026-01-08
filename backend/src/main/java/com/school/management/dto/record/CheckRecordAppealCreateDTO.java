package com.school.management.dto.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建申诉请求DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class CheckRecordAppealCreateDTO {

    /**
     * 扣分明细ID
     */
    @NotNull(message = "扣分明细ID不能为空")
    private Long deductionId;

    /**
     * 申诉理由
     */
    @NotBlank(message = "申诉理由不能为空")
    private String appealReason;

    /**
     * 申诉证据（图片URLs）
     */
    private List<String> appealEvidence;
}
