package com.school.management.interfaces.rest.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating an organization unit.
 */
@Data
@Schema(description = "Create organization unit request")
public class CreateOrgUnitRequest {

    @Schema(description = "Unique code", example = "DEPT_001")
    @NotBlank(message = "Unit code is required")
    @Size(max = 50, message = "Unit code cannot exceed 50 characters")
    private String unitCode;

    @Schema(description = "Display name", example = "Computer Science Department")
    @NotBlank(message = "Unit name is required")
    @Size(max = 100, message = "Unit name cannot exceed 100 characters")
    private String unitName;

    @Schema(description = "Organization type code from org_unit_types", example = "COLLEGE")
    @NotBlank(message = "Unit type is required")
    private String unitType;

    @Schema(description = "Parent organization ID", example = "1")
    private Long parentId;

    @Schema(description = "User-selected positions to create from template")
    private List<SelectedPositionRequest> selectedPositions;

    @Schema(description = "Extension attributes from DynamicForm (stored in org_units.attributes JSON)")
    private Map<String, Object> attributes;

    @Data
    public static class SelectedPositionRequest {
        @Schema(description = "Position name", example = "主任")
        private String positionName;

        @Schema(description = "Headcount for this position", example = "1")
        private int headcount;
    }
}
