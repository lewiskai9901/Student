package com.school.management.interfaces.rest.inspection;

import com.school.management.domain.inspection.model.TemplateScope;
import com.school.management.domain.inspection.model.TemplateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for inspection templates.
 */
@Data
@Schema(description = "Template response")
public class TemplateResponse {

    @Schema(description = "Template ID")
    private Long id;

    @Schema(description = "Template code")
    private String templateCode;

    @Schema(description = "Template name")
    private String templateName;

    @Schema(description = "Template description")
    private String description;

    @Schema(description = "Template scope")
    private TemplateScope scope;

    @Schema(description = "Applicable organization unit ID")
    private Long applicableOrgUnitId;

    @Schema(description = "Is default template")
    private Boolean isDefault;

    @Schema(description = "Current version")
    private Integer currentVersion;

    @Schema(description = "Template status")
    private TemplateStatus status;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Total base score across all categories")
    private Integer totalBaseScore;

    @Schema(description = "Number of categories")
    private Integer categoryCount;
}
