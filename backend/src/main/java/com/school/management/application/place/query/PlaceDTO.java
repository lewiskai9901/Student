package com.school.management.application.place.query;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 场所DTO
 */
@Data
public class PlaceDTO {

    private Long id;
    private String placeCode;
    private String placeName;
    private String placeType;
    private String roomType;
    private String buildingType;

    // 楼号和房间号
    private String buildingNo;          // 楼号（BUILDING类型）
    private String roomNo;              // 房间号（ROOM类型）
    private String parentBuildingNo;    // 所属楼栋的楼号（房间查询时用）

    // 层级
    private Long parentId;
    private String parentName;
    private String path;
    private Integer level;

    // 位置
    private Long campusId;
    private String campusName;
    private Long buildingId;
    private String buildingName;
    private Long floorId;
    private String floorName;
    private Integer floorNumber;

    // 容量
    private Integer capacity;
    private Integer currentOccupancy;
    private Integer availableCapacity;
    private Double occupancyRate;

    // 归属
    private Long orgUnitId;
    private String orgUnitName;
    private String className;               // 归属班级名称
    private Long classTeacherId;            // 班主任ID
    private String classTeacherName;        // 班主任姓名
    private String classTeacherPhone;       // 班主任电话
    private Long responsibleUserId;
    private String responsibleUserName;

    // 性别限制
    private Integer genderType;             // 性别类型：0-不限，1-男，2-女
    private String genderTypeText;          // 性别类型文本

    // 状态
    private Integer status;
    private String statusText;

    // 扩展
    private Map<String, Object> attributes;
    private String description;

    // 子节点（树形结构用）
    private List<PlaceDTO> children;

    // 扩展属性
    private DormitoryExtDTO dormitoryExt;
    private ClassroomExtDTO classroomExt;
    private LabExtDTO labExt;
    private OfficeExtDTO officeExt;

    // 审计
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 宿舍扩展DTO
     */
    @Data
    public static class DormitoryExtDTO {
        private Integer genderType;
        private String genderTypeText;
        private Integer bedCount;
        private String facilities;
        private String assignedClassIds;
        private String assignedClassNames;
        private Long supervisorId;
        private String supervisorName;
    }

    /**
     * 教室扩展DTO
     */
    @Data
    public static class ClassroomExtDTO {
        private String classroomCategory;
        private Long assignedClassId;
        private String assignedClassName;
        private Boolean hasProjector;
        private Boolean hasAirConditioner;
        private Boolean hasComputer;
        private String equipmentInfo;
    }

    /**
     * 实验室扩展DTO
     */
    @Data
    public static class LabExtDTO {
        private String labCategory;
        private Integer safetyLevel;
        private Long majorId;
        private String majorName;
        private String equipmentList;
        private String safetyNotice;
    }

    /**
     * 办公室扩展DTO
     */
    @Data
    public static class OfficeExtDTO {
        private String officeType;
        private Long departmentId;
        private String departmentName;
        private Integer workstationCount;
        private String phoneNumber;
    }
}
