package com.school.management.application.inspection;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Command to record a deduction for a class.
 */
@Data
@Builder
public class RecordDeductionCommand {
    private Long classId;
    private Long deductionItemId;
    private String itemName;
    private Integer count;
    private BigDecimal deductionAmount;
    private String remark;
    private List<String> evidenceUrls;
}
