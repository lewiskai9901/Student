package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场所持久化对象
 */
@Data
@TableName("places")
public class PlacePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String placeCode;
    private String placeName;
    @TableField("type_code")
    private String placeType;
    @TableField(exist = false)
    private Long categoryId;              // V10: 分类ID (not in DB yet)
    @TableField(exist = false)
    private String placeTypeCode;         // 兼容旧字段 - derived from type_code
    private String roomType;              // 兼容旧字段
    @TableField(exist = false)
    private String buildingType;          // not in DB, stored in attributes

    // 楼号和房间号（not in DB, stored in attributes）
    @TableField(exist = false)
    private Integer buildingNo;           // 楼号（数字）- BUILDING类型
    @TableField(exist = false)
    private Integer roomNo;               // 房间号（数字）- ROOM类型
    @TableField(exist = false)
    private Integer floorCount;           // 楼层数 - BUILDING类型

    private Long parentId;
    private String path;
    private Integer level;

    @TableField(exist = false)
    private Long campusId;
    @TableField(exist = false)
    private Long buildingId;
    @TableField(exist = false)
    private Integer floorNumber;

    private Integer capacity;
    private Integer currentOccupancy;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long orgUnitId;

    @TableField(exist = false)
    private Long primaryOrgRelationId;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long responsibleUserId;

    @TableField(exist = false)
    private Integer genderType;        // 性别类型：DB has 'gender' varchar, mapped via XML resultMap

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
