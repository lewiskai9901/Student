package com.school.management.interfaces.rest.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for updating an organization unit.
 */
@Data
@Schema(description = "Update organization unit request")
public class UpdateOrgUnitRequest {

    @Schema(description = "Display name", example = "Computer Science Department")
    @Size(max = 100, message = "Unit name cannot exceed 100 characters")
    private String unitName;

    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder;

    @Schema(description = "Headcount limit")
    private Integer headcount;

    @Schema(description = "Custom attributes (JSON)")
    private Map<String, Object> attributes;

    @Schema(description = "Reason for change")
    private String reason;
}
