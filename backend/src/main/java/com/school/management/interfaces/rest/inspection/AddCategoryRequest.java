package com.school.management.interfaces.rest.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for adding a category to a template.
 */
@Data
@Schema(description = "Add category request")
public class AddCategoryRequest {

    @Schema(description = "Category code", example = "CAT001")
    @NotBlank(message = "Category code is required")
    @Size(max = 50, message = "Category code cannot exceed 50 characters")
    private String categoryCode;

    @Schema(description = "Category name", example = "Cleanliness")
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name cannot exceed 100 characters")
    private String categoryName;

    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Base score for this category", example = "100")
    @NotNull(message = "Base score is required")
    @Min(value = 1, message = "Base score must be at least 1")
    private Integer baseScore;

    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder = 0;
}
