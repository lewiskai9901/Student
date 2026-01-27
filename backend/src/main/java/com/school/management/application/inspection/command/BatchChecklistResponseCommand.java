package com.school.management.application.inspection.command;

import com.school.management.domain.inspection.model.ChecklistResult;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BatchChecklistResponseCommand {
    private Long sessionId;
    private Long classId;
    private List<ChecklistItem> items;

    @Data
    @Builder
    public static class ChecklistItem {
        private Long checklistItemId;
        private String itemName;
        private String categoryName;
        private ChecklistResult result;
        private BigDecimal deductionWhenFail;
        private String inspectorNote;
    }
}
