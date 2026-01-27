package com.school.management.application.inspection.command;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RecordBonusCommand {
    private Long sessionId;
    private Long classId;
    private Long bonusItemId;
    private BigDecimal bonusScore;
    private String reason;
    private Long recordedBy;
}
