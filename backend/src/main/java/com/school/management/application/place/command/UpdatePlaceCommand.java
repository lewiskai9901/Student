package com.school.management.application.space.command;

import lombok.Data;

import java.util.Map;

/**
 * 更新场所命令
 */
@Data
public class UpdateSpaceCommand {

    private Long id;
    private String spaceName;

    // 楼号和房间号
    private String buildingNo;       // 楼号（如 1, A, 甲）- 仅BUILDING类型
    private String roomNo;           // 房间号（如 101, 302）- 仅ROOM类型

    private Integer capacity;
    private Long orgUnitId;
    private Long classId;                // 归属班级ID（直接分配）
    private Long responsibleUserId;
    private String description;
    private Map<String, Object> attributes;

    // 宿舍扩展
    private Integer genderType;
    private Integer bedCount;
    private String facilities;
    private String assignedClassIds;
    private Long supervisorId;

    // 教室扩展
    private String classroomCategory;
    private Long assignedClassId;
    private Boolean hasProjector;
    private Boolean hasAirConditioner;
    private Boolean hasComputer;
    private String equipmentInfo;

    // 实验室扩展
    private String labCategory;
    private Integer safetyLevel;
    private Long majorId;
    private String equipmentList;
    private String safetyNotice;

    // 办公室扩展
    private String officeType;
    private Long departmentId;
    private Integer workstationCount;
    private String phoneNumber;
}
