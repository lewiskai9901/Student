package com.school.management.interfaces.rest.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request DTO for appeal review actions.
 */
@Data
@Schema(description = "Review request")
public class ReviewRequest {

    @Schema(description = "Review comment")
    private String comment;
}
