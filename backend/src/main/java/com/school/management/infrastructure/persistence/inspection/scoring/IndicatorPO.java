package com.school.management.infrastructure.persistence.inspection.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_indicators")
public class IndicatorPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long projectId;
    private Long parentIndicatorId;
    private String name;
    private String indicatorType;
    /** 旧单分区列, 保留过渡; 新代码用 sourceSectionIds JSON */
    private Long sourceSectionId;
    /** JSON 数组 [sectionId, ...] */
    private String sourceSectionIds;
    private String sourceAggregation;
    private String compositeAggregation;
    private String missingPolicy;
    private String normalization;
    private String normalizationConfig;
    /** TIME_WINDOW | COUNT | MANUAL */
    private String triggerMode;
    private Integer countThreshold;
    /** JSON {sectionId: weight} */
    private String weightsBySection;
    /** ASC | DESC | null */
    private String rankDirection;
    /** REVISE_ORIGINAL | CARRY_FORWARD | EXCLUDE */
    private String latePolicy;
    /** taskDate | completedAt */
    private String submissionDateField;
    private String evaluationPeriod;
    private Long gradeSchemeId;
    private String evaluationMethod;
    private String gradeThresholds;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
