package com.school.management.interfaces.rest.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建专业请求
 */
@Data
public class CreateMajorRequest {

    @NotBlank(message = "专业编码不能为空")
    private String majorCode;

    @NotBlank(message = "专业名称不能为空")
    private String majorName;

    @NotNull(message = "所属组织单元不能为空")
    private Long orgUnitId;

    private String description;
}
