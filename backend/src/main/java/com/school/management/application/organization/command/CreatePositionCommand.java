package com.school.management.application.organization.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatePositionCommand {
    private String positionCode;
    private String positionName;
    private Long orgUnitId;
    private Integer headcount;
    private String jobLevel;
    private Long reportsToId;
    private String responsibilities;
    private String requirements;
    private boolean keyPosition;
    private Long createdBy;
}
