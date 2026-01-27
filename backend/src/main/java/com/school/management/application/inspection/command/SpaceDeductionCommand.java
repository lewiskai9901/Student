package com.school.management.application.inspection.command;

import com.school.management.domain.inspection.model.SpaceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SpaceDeductionCommand {
    private Long sessionId;
    private SpaceType spaceType;
    private Long spaceId;
    private String spaceName;
    private Long deductionItemId;
    private String itemName;
    private String categoryName;
    private BigDecimal deductionAmount;
    private Integer personCount;
    private List<Long> studentIds;
    private List<String> studentNames;
    private String remark;
    private List<String> evidenceUrls;
}
