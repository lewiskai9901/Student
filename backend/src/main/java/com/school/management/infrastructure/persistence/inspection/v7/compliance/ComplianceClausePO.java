package com.school.management.infrastructure.persistence.inspection.v7.compliance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_compliance_clauses")
public class ComplianceClausePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long standardId;
    private String clauseNumber;
    private String clauseTitle;
    private String clauseContent;
    private Long parentClauseId;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
