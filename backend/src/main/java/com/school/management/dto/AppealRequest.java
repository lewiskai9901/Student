package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 申诉请求DTO
 *
 * @author system
 * @since 3.1.0
 */
@Data
@Schema(description = "申诉请求")
public class AppealRequest {

    @NotNull(message = "检查记录ID不能为空")
    @Schema(description = "检查记录ID", required = true)
    private Long recordId;

    @NotNull(message = "班级ID不能为空")
    @Schema(description = "班级ID", required = true)
    private Long classId;

    @NotBlank(message = "扣分项描述不能为空")
    @Schema(description = "扣分项描述", required = true)
    private String itemDescription;

    @NotNull(message = "原始扣分不能为空")
    @Schema(description = "原始扣分", required = true)
    private BigDecimal originalScore;

    @NotBlank(message = "申诉理由不能为空")
    @Schema(description = "申诉理由", required = true)
    private String appealReason;

    @Schema(description = "证据图片URLs")
    private List<String> evidenceUrls;
}
