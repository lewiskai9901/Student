package com.school.management.interfaces.rest.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建专业方向请求
 */
@Data
public class CreateMajorDirectionRequest {

    @NotNull(message = "专业ID不能为空")
    private Long majorId;

    @NotBlank(message = "方向编码不能为空")
    private String directionCode;

    @NotBlank(message = "方向名称不能为空")
    private String directionName;

    private String level;
    private Integer years;
    private Integer isSegmented;
    private String phase1Level;
    private Integer phase1Years;
    private String phase2Level;
    private Integer phase2Years;
    private String remarks;
}
