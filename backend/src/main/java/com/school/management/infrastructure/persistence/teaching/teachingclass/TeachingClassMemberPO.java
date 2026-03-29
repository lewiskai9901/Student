package com.school.management.infrastructure.persistence.teaching.teachingclass;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("teaching_class_members")
public class TeachingClassMemberPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teachingClassId;
    private Integer memberType;
    private Long adminClassId;
    private Long studentId;
    private LocalDateTime createdAt;
}
