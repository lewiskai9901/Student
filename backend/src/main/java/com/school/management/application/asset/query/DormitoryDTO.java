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
    private String buildingNo;
    private String buildingName;
    private Long orgUnitId;
    private String orgUnitName;
    private String dormitoryNo;

    // 宿舍管理员信息
    private Long supervisorId;
    private String supervisorName;

    // 指定班级信息
    private String assignedClassIds;
    private String assignedClassNames;

    // 班主任信息（从入住学生的班级获取）
    private String classTeacherNames;
    private String classTeacherPhones;

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
