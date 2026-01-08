package com.school.management.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 申诉创建请求
 *
 * @author system
 * @since 1.0.6
 */
@Data
public class AppealCreateRequest {

    /**
     * 明细ID
     */
    @NotNull(message = "明细ID不能为空")
    private Long detailId;

    /**
     * 申诉原因
     */
    @NotBlank(message = "申诉原因不能为空")
    private String appealReason;

    /**
     * 申诉人ID
     */
    @NotNull(message = "申诉人ID不能为空")
    private Long appealUserId;

    /**
     * 申诉人姓名
     */
    private String appealUserName;

    /**
     * 申诉照片URL(多个用逗号分隔)
     */
    private String appealPhotoUrls;
}
