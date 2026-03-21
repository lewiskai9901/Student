package com.school.management.domain.evaluation.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评选执行批次
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalBatch {

    private Long id;
    private Long tenantId;
    private Long campaignId;
    private LocalDate cycleStart;
    private LocalDate cycleEnd;
    private Integer totalTargets;
    private LocalDateTime executedAt;
    private Long executedBy;
    /** COMPLETED / FAILED / RUNNING */
    private String status;
    /** JSON: {level1Count:3, level2Count:12, noLevelCount:5} */
    private String summary;
    private LocalDateTime createdAt;

    public void setId(Long id) { this.id = id; }
}
