package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cohort_semester_mapping")
public class CohortSemesterMappingPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long cohortId;
    private Long semesterId;
    private Integer programSemester;
    private Long planId;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
