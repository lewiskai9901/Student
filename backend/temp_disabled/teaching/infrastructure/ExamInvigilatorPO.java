package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 监考教师持久化对象
 */
@Data
@TableName("exam_invigilators")
public class ExamInvigilatorPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long roomId;

    private Long teacherId;

    /**
     * 是否主监考
     */
    private Boolean isMain;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
