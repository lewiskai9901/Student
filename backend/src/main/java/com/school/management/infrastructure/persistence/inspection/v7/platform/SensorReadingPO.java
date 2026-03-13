package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_sensor_readings")
public class SensorReadingPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sensorId;
    private BigDecimal readingValue;
    private String readingUnit;
    private LocalDateTime recordedAt;
}
