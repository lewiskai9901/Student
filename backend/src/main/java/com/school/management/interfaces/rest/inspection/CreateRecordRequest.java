package com.school.management.interfaces.rest.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO for creating a new inspection record.
 */
@Data
@Schema(description = "Create record request")
public class CreateRecordRequest {

    @Schema(description = "Template ID to use for this inspection")
    @NotNull(message = "Template ID is required")
    private Long templateId;

    @Schema(description = "Round ID (inspection cycle)")
    private Long roundId;

    @Schema(description = "Inspection date")
    @NotNull(message = "Inspection date is required")
    private LocalDate inspectionDate;

    @Schema(description = "Inspection period", example = "MORNING")
    private String inspectionPeriod;
}
