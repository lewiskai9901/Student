package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 楼宇持久化对象
 */
@Data
@TableName("buildings")
public class BuildingPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String buildingNo;

    private String buildingName;

    private Integer buildingType;

    private Integer totalFloors;

    private String location;

    private Integer constructionYear;

    private String description;

    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
