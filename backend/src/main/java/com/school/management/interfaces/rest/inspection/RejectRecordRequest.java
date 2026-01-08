package com.school.management.interfaces.rest.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for rejecting an inspection record.
 */
@Data
@Schema(description = "Reject record request")
public class RejectRecordRequest {

    @Schema(description = "Rejection reason")
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
