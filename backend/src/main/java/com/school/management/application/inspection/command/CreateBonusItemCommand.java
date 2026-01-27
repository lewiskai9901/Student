package com.school.management.application.inspection.command;

import com.school.management.domain.inspection.model.BonusMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateBonusItemCommand {
    private Long categoryId;
    private String itemName;
    private BonusMode bonusMode;
    private BigDecimal fixedBonus;
    private String progressiveConfig;
    private BigDecimal improvementCoefficient;
    private String description;
    private Integer sortOrder;
}
