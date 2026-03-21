package com.school.management.domain.evaluation.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评选条件值对象
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalCondition {

    private Long id;
    private Long levelId;
    /** INSPECTION / EVENT / HISTORY */
    private String sourceType;
    /** JSON: 数据源参数（projectId, sectionId, eventType, campaignId...） */
    private String sourceConfig;
    /** SCORE_AVG / SCORE_MIN / FAIL_COUNT / COUNT / PREV_LEVEL / CONSECUTIVE ... */
    private String metric;
    /** >= / <= / = / != / IN */
    private String operator;
    /** 阈值（数字字符串或 JSON 数组字符串） */
    private String threshold;
    /** SELF / MEMBERS / SPECIFIC_ROLE */
    private String scope;
    private String scopeRole;
    /** CYCLE / CUSTOM */
    private String timeRange;
    private Integer timeRangeDays;
    /** 自然语言描述（自动生成） */
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public void setId(Long id) { this.id = id; }
}
