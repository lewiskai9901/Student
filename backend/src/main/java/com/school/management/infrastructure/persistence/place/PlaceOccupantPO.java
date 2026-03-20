package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 场所占用持久化对象
 */
@Data
@TableName("place_occupant")
public class PlaceOccupantPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long placeId;
    private String occupantType;
    private Long occupantId;
    private Integer positionNo;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 关联查询字段（非数据库字段）
    @TableField(exist = false)
    private String occupantName;
    @TableField(exist = false)
    private String occupantNo;
    @TableField(exist = false)
    private String placeName;
}
