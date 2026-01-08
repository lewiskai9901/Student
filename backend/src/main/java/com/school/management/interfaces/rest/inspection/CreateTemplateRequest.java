package com.school.management.interfaces.rest.inspection;

import com.school.management.domain.inspection.model.TemplateScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a new inspection template.
 */
@Data
@Schema(description = "Create template request")
public class CreateTemplateRequest {

    @Schema(description = "Template code", example = "TPL001")
    @NotBlank(message = "Template code is required")
    @Size(max = 50, message = "Template code cannot exceed 50 characters")
    private String templateCode;

    @Schema(description = "Template name", example = "Daily Inspection Template")
    @NotBlank(message = "Template name is required")
    @Size(max = 100, message = "Template name cannot exceed 100 characters")
    private String templateName;

    @Schema(description = "Template description")
    private String description;

    @Schema(description = "Template scope", example = "GLOBAL")
    private TemplateScope scope = TemplateScope.GLOBAL;

    @Schema(description = "Applicable organization unit ID (for DEPARTMENT or CLASS scope)")
    private Long applicableOrgUnitId;
}
