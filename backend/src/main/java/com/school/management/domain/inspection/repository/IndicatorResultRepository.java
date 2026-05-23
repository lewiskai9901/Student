package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.evaluation.IndicatorResult;
import com.school.management.domain.inspection.model.evaluation.ResultStatus;

import java.util.List;
import java.util.Optional;

public interface IndicatorResultRepository {

    IndicatorResult save(IndicatorResult result);

    Optional<IndicatorResult> findById(Long id);

    List<IndicatorResult> findByIndicatorId(Long indicatorId);

    List<IndicatorResult> findByIndicatorIdAndStatus(Long indicatorId, ResultStatus status);

    Optional<IndicatorResult> findCurrentPublished(Long indicatorId, Long targetId, String periodKey);

    /** 修订链: 给定 head id, 找其所有祖先 (revisionOf 链向上) */
    List<IndicatorResult> findRevisionHistory(Long indicatorId, Long targetId, String periodKey);

    void deleteById(Long id);
}
