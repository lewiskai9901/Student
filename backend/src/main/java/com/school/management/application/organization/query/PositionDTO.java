package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.List;

@Data
public class PositionDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String positionCode;
    private String positionName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgUnitId;
    private String orgUnitName;
    private String jobLevel;
    private int headcount;
    private int currentCount;
    private int vacantCount;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reportsToId;
    private String reportsToName;
    private String responsibilities;
    private String requirements;
    private boolean keyPosition;
    private boolean enabled;
    private List<UserPositionSummaryDTO> holders;

    @Data
    public static class UserPositionSummaryDTO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String userName;
        private boolean primary;
        private String appointmentType;
    }
}
