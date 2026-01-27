package com.school.management.interfaces.rest.organization;

import com.school.management.domain.organization.model.UnitCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for updating an organization unit.
 */
@Data
@Schema(description = "Update organization unit request")
public class UpdateOrgUnitRequest {

    @Schema(description = "Display name", example = "Computer Science Department")
    @Size(max = 100, message = "Unit name cannot exceed 100 characters")
    private String unitName;

    @Schema(description = "Organization category: ACADEMIC, FUNCTIONAL, ADMINISTRATIVE")
    private UnitCategory unitCategory;

    @Schema(description = "Leader user ID", example = "100")
    private Long leaderId;

    @Schema(description = "Deputy leader user IDs")
    private List<Long> deputyLeaderIds;

    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder;
}
