package com.school.management.domain.evaluation.repository;

import com.school.management.domain.evaluation.model.EvalResult;

import java.util.List;

/**
 * 评选结果仓储接口
 */
public interface EvalResultRepository {

    EvalResult save(EvalResult result);

    void saveAll(List<EvalResult> results);

    List<EvalResult> findByBatchId(Long batchId);

    List<EvalResult> findByCampaignAndTarget(Long campaignId, String targetType, Long targetId);

    /** 查询某活动最近 N 批次的某目标结果（用于 CONSECUTIVE/TREND 计算） */
    List<EvalResult> findRecentByTarget(Long campaignId, Long targetId, int limit);

    void deleteByBatchId(Long batchId);
}
