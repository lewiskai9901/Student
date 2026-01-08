package com.school.management.application.asset.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宿舍查询DTO
 */
@Data
@Builder
public class DormitoryDTO {

    private Long id;
    private Long buildingId;
    private String buildingName;
    private Long departmentId;
    private String departmentName;
    private String dormitoryNo;
    private Integer floorNumber;
    private Integer roomUsageType;
    private String roomUsageTypeName;
    private Integer bedCapacity;
    private Integer bedCount;
    private Integer occupiedBeds;
    private Integer availableBeds;
    private Integer genderType;
    private String genderTypeName;
    private String facilities;
    private String notes;
    private Integer status;
    private String statusName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
