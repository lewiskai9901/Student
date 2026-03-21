package com.school.management.domain.evaluation.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评选批次结果
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalResult {

    private Long id;
    private Long batchId;
    private Long campaignId;
    private String targetType;
    private Long targetId;
    private String targetName;
    /** NULL = 未达任何级别 */
    private Integer levelNum;
    private String levelName;
    private Integer rankNo;
    /** 综合分（用于排名） */
    private BigDecimal score;
    /** JSON: [{conditionId, passed, actual, threshold, description}] */
    private String conditionDetails;
    /** 升级提示文本 */
    private String upgradeHint;
    private LocalDateTime createdAt;

    public void setId(Long id) { this.id = id; }
}
