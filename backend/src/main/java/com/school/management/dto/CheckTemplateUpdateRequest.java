package com.school.management.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotNull;

/**
 * 更新检查模板请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CheckTemplateUpdateRequest extends CheckTemplateCreateRequest {

    /**
     * 模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private Long id;
}
