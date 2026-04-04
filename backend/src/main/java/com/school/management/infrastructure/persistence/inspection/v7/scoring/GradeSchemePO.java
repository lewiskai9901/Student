package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_grade_schemes")
public class GradeSchemePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String displayName;
    private String description;
    private String schemeType;
    private Boolean isSystem;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
