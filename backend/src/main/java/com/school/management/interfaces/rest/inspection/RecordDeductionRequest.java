package com.school.management.interfaces.rest.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for recording a deduction.
 */
@Data
@Schema(description = "Record deduction request")
public class RecordDeductionRequest {

    @Schema(description = "Class ID")
    @NotNull(message = "Class ID is required")
    private Long classId;

    @Schema(description = "Deduction item ID")
    @NotNull(message = "Deduction item ID is required")
    private Long deductionItemId;

    @Schema(description = "Deduction item name")
    @NotBlank(message = "Item name is required")
    private String itemName;

    @Schema(description = "Count of violations", example = "1")
    @NotNull(message = "Count is required")
    @Min(value = 1, message = "Count must be at least 1")
    private Integer count;

    @Schema(description = "Deduction amount", example = "5.0")
    @NotNull(message = "Deduction amount is required")
    private BigDecimal deductionAmount;

    @Schema(description = "Remark or note")
    private String remark;

    @Schema(description = "Evidence URLs (photos)")
    private List<String> evidenceUrls;
}
