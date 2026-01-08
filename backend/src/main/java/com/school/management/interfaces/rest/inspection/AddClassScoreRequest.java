package com.school.management.interfaces.rest.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for adding a class score to an inspection record.
 */
@Data
@Schema(description = "Add class score request")
public class AddClassScoreRequest {

    @Schema(description = "Class ID")
    @NotNull(message = "Class ID is required")
    private Long classId;

    @Schema(description = "Class name")
    @NotBlank(message = "Class name is required")
    private String className;

    @Schema(description = "Base score for this class", example = "100")
    @NotNull(message = "Base score is required")
    @Min(value = 0, message = "Base score cannot be negative")
    private Integer baseScore;
}
