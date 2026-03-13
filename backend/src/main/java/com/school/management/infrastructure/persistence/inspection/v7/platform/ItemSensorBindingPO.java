package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_item_sensor_bindings")
public class ItemSensorBindingPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long templateItemId;
    private Long sensorId;
    private Boolean autoFill;
    private Boolean autoScore;
    private String scoringThresholds;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
