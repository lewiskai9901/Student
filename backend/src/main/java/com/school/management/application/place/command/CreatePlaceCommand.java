package com.school.management.application.place.command;

import com.school.management.domain.place.model.valueobject.BuildingType;
import com.school.management.domain.place.model.valueobject.RoomType;
import com.school.management.domain.place.model.valueobject.PlaceType;
import lombok.Data;

import java.util.Map;

/**
 * 创建场所命令
 */
@Data
public class CreatePlaceCommand {

    private String placeCode;        // 可选，不填则自动生成
    private String placeName;
    private PlaceType placeType;
    private RoomType roomType;       // 仅ROOM有效
    private BuildingType buildingType; // 仅BUILDING有效

    // 楼号和房间号
    private String buildingNo;       // 楼号（如 1, A, 甲）- 仅BUILDING类型
    private String roomNo;           // 房间号（如 101, 302）- 仅ROOM类型

    private Long parentId;
    private Integer floorNumber;     // 楼层号（创建楼层或房间时使用）

    private Integer capacity;

    private Long orgUnitId;
    private Long orgUnitId;                // 归属班级ID（直接分配）
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
