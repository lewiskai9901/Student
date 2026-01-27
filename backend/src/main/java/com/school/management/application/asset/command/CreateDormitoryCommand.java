package com.school.management.application.asset.command;

import lombok.Builder;
import lombok.Data;

/**
 * 创建宿舍命令
 */
@Data
@Builder
public class CreateDormitoryCommand {

    private Long buildingId;
    private Long orgUnitId;
    private String dormitoryNo;
    private Integer floorNumber;
    private Integer roomUsageType;
    private Integer bedCapacity;
    private Integer genderType;
    private String facilities;
    private String notes;
}
