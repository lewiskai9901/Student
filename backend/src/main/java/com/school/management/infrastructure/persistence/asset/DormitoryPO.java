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

    /**
     * 组织单元ID（部门）- 允许更新为null以支持取消分配
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long orgUnitId;

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
