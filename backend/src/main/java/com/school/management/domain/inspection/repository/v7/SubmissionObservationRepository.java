package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.execution.ScoringObservation;

import java.util.List;

/**
 * 评分观察仓储接口
 */
public interface SubmissionObservationRepository {

    void batchInsert(List<ScoringObservation> observations);

    List<ScoringObservation> findBySubmissionId(Long submissionId);

    List<ScoringObservation> findNegativeBySubmissionId(Long submissionId);

    void deleteBySubmissionId(Long submissionId);
}
