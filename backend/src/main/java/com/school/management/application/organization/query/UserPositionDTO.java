package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class UserPositionDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String userName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long positionId;
    private String positionName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgUnitId;
    private String orgUnitName;
    private boolean primary;
    private String appointmentType;
    private String startDate;
    private String endDate;
    private String appointmentReason;
    private String departureReason;
    private boolean current;
}
