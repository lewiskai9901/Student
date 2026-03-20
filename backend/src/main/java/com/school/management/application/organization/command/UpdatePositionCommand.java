package com.school.management.application.organization.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdatePositionCommand {
    private String positionName;
    private String jobLevel;
    private Integer headcount;
    private Long reportsToId;
    private String responsibilities;
    private String requirements;
    private boolean keyPosition;
    private Integer sortOrder;
    private Long updatedBy;
}
