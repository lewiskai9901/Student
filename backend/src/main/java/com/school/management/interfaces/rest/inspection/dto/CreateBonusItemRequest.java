package com.school.management.interfaces.rest.inspection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateBonusItemRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotBlank(message = "Bonus mode is required")
    private String bonusMode;

    private BigDecimal fixedBonus;
    private String progressiveConfig;
    private BigDecimal improvementCoefficient;
    private String description;
    private Integer sortOrder;
}
