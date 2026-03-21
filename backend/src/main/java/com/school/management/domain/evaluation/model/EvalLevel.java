package com.school.management.domain.evaluation.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评选级别实体（1 = 最高级别）
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalLevel {

    private Long id;
    private Long campaignId;
    /** 级别序号，1 = 最高 */
    private Integer levelNum;
    private String levelName;
    /** AND / OR */
    private String conditionLogic;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 非持久化：条件列表（查询时填充） */
    private List<EvalCondition> conditions;

    public void setId(Long id) { this.id = id; }
    public void setConditions(List<EvalCondition> conditions) { this.conditions = conditions; }
}
