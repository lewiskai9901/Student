package com.school.management.interfaces.rest.inspection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PersonDeductionRequest {

    @NotEmpty(message = "Student IDs are required")
    private List<Long> studentIds;

    private List<String> studentNames;

    private Long deductionItemId;

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String categoryName;

    @NotNull(message = "Deduction amount is required")
    private BigDecimal deductionAmount;

    private String remark;
    private List<String> evidenceUrls;
}
