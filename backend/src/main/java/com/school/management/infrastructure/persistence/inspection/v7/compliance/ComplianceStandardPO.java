package com.school.management.infrastructure.persistence.inspection.v7.compliance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_compliance_standards")
public class ComplianceStandardPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String standardCode;
    private String standardName;
    private String issuingBody;
    private LocalDate effectiveDate;
    private String version;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
