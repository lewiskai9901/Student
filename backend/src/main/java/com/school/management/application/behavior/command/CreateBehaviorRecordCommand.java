package com.school.management.application.behavior.command;

import com.school.management.domain.behavior.model.BehaviorCategory;
import com.school.management.domain.behavior.model.BehaviorSource;
import com.school.management.domain.behavior.model.BehaviorType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateBehaviorRecordCommand {
    private Long studentId;
    private Long classId;
    private BehaviorType behaviorType;
    private BehaviorSource source;
    private Long sourceId;
    private BehaviorCategory category;
    private String title;
    private String detail;
    private BigDecimal deductionAmount;
    private Long recordedBy;
}
