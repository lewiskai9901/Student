package com.school.management.domain.evaluation.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评选活动聚合根
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalCampaign {

    private Long id;
    private Long tenantId;
    private String campaignName;
    private String campaignDescription;
    /** ORG / PLACE / USER */
    private String targetType;
    /** JSON: 范围组织ID列表 */
    private String scopeOrgIds;
    /** MONTHLY / WEEKLY / TERM */
    private String evaluationPeriod;
    /** DRAFT / ACTIVE / PAUSED / ARCHIVED */
    private String status;
    private Boolean isAutoExecute;
    private LocalDateTime lastExecutedAt;
    private LocalDateTime nextExecuteAt;
    private Integer sortOrder;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 非持久化：级别列表（查询时填充） */
    private List<EvalLevel> levels;

    public static EvalCampaign create(String campaignName, String targetType, Long createdBy) {
        return EvalCampaign.builder()
                .campaignName(campaignName)
                .targetType(targetType)
                .evaluationPeriod("MONTHLY")
                .status("DRAFT")
                .isAutoExecute(false)
                .sortOrder(0)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void setId(Long id) { this.id = id; }
    public void setLevels(List<EvalLevel> levels) { this.levels = levels; }
}
