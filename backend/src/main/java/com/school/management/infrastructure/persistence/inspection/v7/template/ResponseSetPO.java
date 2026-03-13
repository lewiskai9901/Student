package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_response_sets")
public class ResponseSetPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String setCode;
    private String setName;
    private Boolean isGlobal;
    private Boolean isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
