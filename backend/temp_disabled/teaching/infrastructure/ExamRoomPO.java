package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试教室持久化对象
 */
@Data
@TableName("exam_rooms")
public class ExamRoomPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long arrangementId;

    private Long classroomId;

    /**
     * 容纳人数
     */
    private Integer capacity;

    /**
     * 实际考生数
     */
    private Integer actualCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
