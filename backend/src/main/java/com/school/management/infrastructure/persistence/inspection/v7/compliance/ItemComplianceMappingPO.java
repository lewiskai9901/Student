package com.school.management.infrastructure.persistence.inspection.v7.compliance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_item_compliance_mappings")
public class ItemComplianceMappingPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateItemId;
    private Long clauseId;
    private String coverageLevel;
    private String notes;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
