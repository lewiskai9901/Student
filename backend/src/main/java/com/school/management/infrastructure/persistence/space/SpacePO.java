package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场所持久化对象
 */
@Data
@TableName("space")
public class SpacePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String spaceCode;
    private String spaceName;
    private String spaceType;
    private String roomType;
    private String buildingType;

    // 楼号和房间号
    private String buildingNo;      // 楼号（如 1, A, 甲）- BUILDING类型
    private String roomNo;          // 房间号（如 101, 302）- ROOM类型

    private Long parentId;
    private String path;
    private Integer level;

    private Long campusId;
    private Long buildingId;
    private Integer floorNumber;

    private Integer capacity;
    private Integer currentOccupancy;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long orgUnitId;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long classId;              // 归属班级ID

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long responsibleUserId;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Integer genderType;        // 性别类型：0-不限，1-男，2-女

    private Integer status;

    private String attributes;  // JSON字符串

    private String description;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    // 关联查询字段（非数据库字段）
    @TableField(exist = false)
    private String parentName;
    @TableField(exist = false)
    private String campusName;
    @TableField(exist = false)
    private String buildingName;
    @TableField(exist = false)
    private String parentBuildingNo;    // 所属楼栋的楼号（房间查询时用）
    @TableField(exist = false)
    private Long floorId;               // 所属楼层ID
    @TableField(exist = false)
    private String floorName;           // 所属楼层名称
    @TableField(exist = false)
    private String orgUnitName;
    @TableField(exist = false)
    private String className;              // 归属班级名称
    @TableField(exist = false)
    private Long classTeacherId;           // 班主任ID
    @TableField(exist = false)
    private String classTeacherName;       // 班主任姓名
    @TableField(exist = false)
    private String classTeacherPhone;      // 班主任电话
    @TableField(exist = false)
    private String responsibleUserName;
}
