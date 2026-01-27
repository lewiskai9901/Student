package com.school.management.interfaces.rest.inspection.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BatchChecklistRequest {

    @NotNull(message = "Class ID is required")
    private Long classId;

    private List<ChecklistItem> items;

    @Data
    public static class ChecklistItem {
        private Long checklistItemId;
        private String itemName;
        private String categoryName;
        private String result;
        private BigDecimal deductionWhenFail;
        private String inspectorNote;
    }
}
