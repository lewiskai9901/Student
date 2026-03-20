package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("booking_seat_assignments")
public class BookingSeatAssignmentPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookingId;

    private String positionNo;

    private Long userId;

    private String userName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 不使用逻辑删除 — 座位分配为临时数据，物理删除避免唯一键冲突
    private Integer deleted;
}
