package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建专业请求DTO
 */
@Data
public class MajorCreateRequest {

    /**
     * 专业名称
     */
    @NotBlank(message = "专业名称不能为空")
    private String majorName;

    /**
     * 专业编码
     */
    @NotBlank(message = "专业编码不能为空")
    private String majorCode;

    /**
     * 所属组织单元ID
     */
    @NotNull(message = "所属组织单元不能为空")
    private Long orgUnitId;

    /**
     * 专业描述
     */
    private String description;

    /**
     * 状态(0-禁用 1-启用)
     */
    private Integer status;
}
