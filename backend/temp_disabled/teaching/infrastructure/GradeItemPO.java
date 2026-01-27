package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_grade_items")
public class GradeItemPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long gradeId;
    private String itemName;
    private BigDecimal score;
    private Integer weight;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
