package com.school.management.dto.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 处理申诉请求DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class CheckRecordAppealHandleDTO {

    /**
     * 申诉ID
     */
    @NotNull(message = "申诉ID不能为空")
    private Long appealId;

    /**
     * 处理结果：true=通过 false=驳回
     */
    @NotNull(message = "处理结果不能为空")
    private Boolean approved;

    /**
     * 处理结果说明
     */
    @NotBlank(message = "处理说明不能为空")
    private String handleResult;

    /**
     * 调整后扣分（通过时必填）
     */
    private BigDecimal adjustedScore;
}
