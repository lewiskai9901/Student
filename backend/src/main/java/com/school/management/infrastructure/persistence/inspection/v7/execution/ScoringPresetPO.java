package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_scoring_presets")
public class ScoringPresetPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;
    private String presetName;
    private String presetType;
    private String itemValues;
    private Integer usageCount;
    private Long createdBy;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
