package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_iot_sensors")
public class IoTSensorPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String sensorCode;
    private String sensorName;
    private String sensorType;
    private String locationName;
    private Long placeId;
    private String mqttTopic;
    private String dataUnit;
    private Boolean isActive;
    private BigDecimal lastReading;
    private LocalDateTime lastReadingAt;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
