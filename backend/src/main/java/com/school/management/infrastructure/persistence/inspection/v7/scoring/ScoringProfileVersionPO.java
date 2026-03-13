package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_scoring_profile_versions")
public class ScoringProfileVersionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long profileId;
    private Integer version;
    private String snapshot;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private String changeSummary;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
