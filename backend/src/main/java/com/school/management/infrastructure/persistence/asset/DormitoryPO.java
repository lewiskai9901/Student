package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宿舍持久化对象
 */
@Data
@TableName("dormitories")
public class DormitoryPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long buildingId;

    private Long departmentId;

    private String dormitoryNo;

    private Integer floorNumber;

    private Integer roomUsageType;

    private Integer bedCapacity;

    private Integer bedCount;

    private Integer occupiedBeds;

    private Integer genderType;

    private String facilities;

    private String notes;

    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
