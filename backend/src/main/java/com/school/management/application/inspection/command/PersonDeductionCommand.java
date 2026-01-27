package com.school.management.application.inspection.command;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PersonDeductionCommand {
    private Long sessionId;
    private List<Long> studentIds;
    private List<String> studentNames;
    private Long deductionItemId;
    private String itemName;
    private String categoryName;
    private BigDecimal deductionAmount;
    private String remark;
    private List<String> evidenceUrls;
}
