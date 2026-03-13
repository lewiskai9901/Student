package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_escalation_policies")
public class EscalationPolicyPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long profileId;
    private String policyName;
    private Integer lookupPeriodDays;
    private String escalationMode;
    private BigDecimal multiplier;
    private BigDecimal adder;
    private String fixedTable;
    private BigDecimal maxEscalationFactor;
    private String matchBy;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
