package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_response_set_options")
public class ResponseSetOptionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long responseSetId;
    private String optionValue;
    private String optionLabel;
    private String optionColor;
    private BigDecimal score;
    private Boolean isFlagged;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
