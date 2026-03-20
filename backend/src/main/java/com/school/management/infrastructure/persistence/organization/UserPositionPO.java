package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_positions")
public class UserPositionPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("position_id")
    private Long positionId;
    @TableField("is_primary")
    private Boolean isPrimary;
    @TableField("appointment_type")
    private String appointmentType;
    @TableField("start_date")
    private LocalDate startDate;
    @TableField("end_date")
    private LocalDate endDate;
    @TableField("appointment_reason")
    private String appointmentReason;
    @TableField("departure_reason")
    private String departureReason;
    @TableField("tenant_id")
    private Long tenantId;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("created_by")
    private Long createdBy;
}
