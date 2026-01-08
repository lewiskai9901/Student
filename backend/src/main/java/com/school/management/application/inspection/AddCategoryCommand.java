package com.school.management.application.inspection;

import lombok.Builder;
import lombok.Data;

/**
 * Command to add a category to an inspection template.
 */
@Data
@Builder
public class AddCategoryCommand {
    private String categoryCode;
    private String categoryName;
    private String description;
    private Integer baseScore;
    private Integer sortOrder;
}
