package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.execution.ScoringObservation;

import java.util.List;

/**
 * 评分观察仓储接口
 */
public interface SubmissionObservationRepository {

    void batchInsert(List<ScoringObservation> observations);

    List<ScoringObservation> findBySubmissionId(Long submissionId);

    List<ScoringObservation> findNegativeBySubmissionId(Long submissionId);

    void deleteBySubmissionId(Long submissionId);

    /**
     * 统计同一主体在指定回溯期内同 itemCode 的负面观察次数 — 用于重复违规递增扣分.
     *
     * @param subjectType  USER / ORG_UNIT / PLACE / ASSET
     * @param subjectId    主体 ID
     * @param itemCode     检查项编码
     * @param sinceUtcDays 回溯天数 (不含当天)
     * @return 期间内负面观察数 (is_negative=1, 非软删)
     */
    long countNegativeForSubjectInPeriod(String subjectType, Long subjectId,
                                          String itemCode, int sinceUtcDays);
}
