package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场所-班级分配持久化对象
 */
@Data
@TableName("place_class_assignment")
public class PlaceClassAssignmentPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long placeId;
    private Long orgUnitId;
    private Long orgUnitId;
    private Integer assignedBeds;
    private Integer priority;
    private Integer status;      // 0-禁用，1-启用
    private String remark;
    private Long assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 关联查询字段（非数据库字段）
    @TableField(exist = false)
    private String placeName;
    @TableField(exist = false)
    private String placeCode;
    @TableField(exist = false)
    private String placeType;
    @TableField(exist = false)
    private Integer placeCapacity;
    @TableField(exist = false)
    private String className;
    @TableField(exist = false)
    private String classCode;
    @TableField(exist = false)
    private Long teacherId;
    @TableField(exist = false)
    private String teacherName;
    @TableField(exist = false)
    private String teacherPhone;
    @TableField(exist = false)
    private String orgUnitName;
}
