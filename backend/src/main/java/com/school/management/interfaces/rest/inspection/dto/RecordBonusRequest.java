package com.school.management.interfaces.rest.inspection.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecordBonusRequest {

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Bonus item ID is required")
    private Long bonusItemId;

    @NotNull(message = "Bonus score is required")
    private BigDecimal bonusScore;

    private String reason;
}
