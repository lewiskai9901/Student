package com.school.management.domain.evaluation.repository;

import com.school.management.domain.evaluation.model.EvalCampaign;

import java.util.List;
import java.util.Optional;

/**
 * 评选活动仓储接口
 */
public interface EvalCampaignRepository {

    EvalCampaign save(EvalCampaign campaign);

    Optional<EvalCampaign> findById(Long id);

    /** 查询时包含 levels + conditions */
    Optional<EvalCampaign> findByIdWithLevels(Long id);

    List<EvalCampaign> findAll();

    List<EvalCampaign> findByStatus(String status);

    void deleteById(Long id);
}
