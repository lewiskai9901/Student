package com.school.management.interfaces.rest.inspection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SpaceDeductionRequest {

    @NotBlank(message = "Space type is required")
    private String spaceType;

    @NotNull(message = "Space ID is required")
    private Long spaceId;

    private String spaceName;

    private Long deductionItemId;

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String categoryName;

    @NotNull(message = "Deduction amount is required")
    private BigDecimal deductionAmount;

    private Integer personCount;
    private List<Long> studentIds;
    private List<String> studentNames;
    private String remark;
    private List<String> evidenceUrls;
}
