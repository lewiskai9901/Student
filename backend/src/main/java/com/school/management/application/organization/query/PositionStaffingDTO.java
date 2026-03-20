package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.List;

@Data
public class PositionStaffingDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgUnitId;
    private String orgUnitName;
    private int totalHeadcount;
    private int actualCount;
    private int vacantCount;
    private int overstaffedCount;
    private List<PositionStaffingItem> positions;

    @Data
    public static class PositionStaffingItem {
        private String positionName;
        private int headcount;
        private int actualCount;
    }
}
