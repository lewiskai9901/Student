package com.school.management.interfaces.rest.academic;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建年级-方向关联请求
 */
@Data
public class CreateGradeMajorDirectionRequest {

    @NotNull(message = "学年不能为空")
    private Integer academicYear;

    @NotNull(message = "专业方向ID不能为空")
    private Long majorDirectionId;

    private String remarks;
}
