package com.school.management.domain.evaluation.repository;

import com.school.management.domain.evaluation.model.EvalBatch;

import java.util.List;
import java.util.Optional;

/**
 * 评选执行批次仓储接口
 */
public interface EvalBatchRepository {

    EvalBatch save(EvalBatch batch);

    Optional<EvalBatch> findById(Long id);

    List<EvalBatch> findByCampaignId(Long campaignId);
}
