package com.school.management.interfaces.rest.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for creating an organization unit.
 */
@Data
@Schema(description = "Create organization unit request")
public class CreateOrgUnitRequest {

    @Schema(description = "Unique code", example = "DEPT_001")
    @NotBlank(message = "组织编码不能为空")
    @Size(min = 2, max = 50, message = "组织编码长度须在2-50之间")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "组织编码只能包含字母、数字和下划线")
    private String unitCode;

    @Schema(description = "Display name", example = "Computer Science Department")
    @NotBlank(message = "组织名称不能为空")
    @Size(max = 100, message = "组织名称不能超过100个字符")
    private String unitName;

    @Schema(description = "Organization type code from org_unit_types", example = "COLLEGE")
    @NotBlank(message = "组织类型不能为空")
    private String unitType;

    @Schema(description = "Parent organization ID (null for root node)", example = "1")
    private Long parentId;

    @Schema(description = "Extension attributes from DynamicForm (stored in org_units.attributes JSON)")
    private Map<String, Object> attributes;
}
