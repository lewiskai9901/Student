package com.school.management.infrastructure.persistence.evaluation;

import com.school.management.domain.evaluation.model.EvalResult;
import com.school.management.domain.evaluation.repository.EvalResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评选批次结果仓储实现
 */
@Repository
@RequiredArgsConstructor
public class EvalResultRepositoryImpl implements EvalResultRepository {

    private final EvalResultMapper resultMapper;

    @Override
    public EvalResult save(EvalResult result) {
        EvalResultPO po = toPO(result);
        if (result.getId() == null) {
            resultMapper.insert(po);
            result.setId(po.getId());
        } else {
            resultMapper.updateById(po);
        }
        return result;
    }

    @Override
    public void saveAll(List<EvalResult> results) {
        for (EvalResult r : results) {
            save(r);
        }
    }

    @Override
    public List<EvalResult> findByBatchId(Long batchId) {
        return resultMapper.findByBatchId(batchId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EvalResult> findByCampaignAndTarget(Long campaignId, String targetType, Long targetId) {
        return resultMapper.findByCampaignAndTarget(campaignId, targetType, targetId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EvalResult> findRecentByTarget(Long campaignId, Long targetId, int limit) {
        return resultMapper.findRecentByTarget(campaignId, targetId, limit).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByBatchId(Long batchId) {
        resultMapper.deleteByBatchId(batchId);
    }

    private EvalResultPO toPO(EvalResult d) {
        EvalResultPO po = new EvalResultPO();
        po.setId(d.getId());
        po.setBatchId(d.getBatchId());
        po.setCampaignId(d.getCampaignId());
        po.setTargetType(d.getTargetType());
        po.setTargetId(d.getTargetId());
        po.setTargetName(d.getTargetName());
        po.setLevelNum(d.getLevelNum());
        po.setLevelName(d.getLevelName());
        po.setRankNo(d.getRankNo());
        po.setScore(d.getScore());
        po.setConditionDetails(d.getConditionDetails());
        po.setUpgradeHint(d.getUpgradeHint());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private EvalResult toDomain(EvalResultPO po) {
        return EvalResult.builder()
                .id(po.getId())
                .batchId(po.getBatchId())
                .campaignId(po.getCampaignId())
                .targetType(po.getTargetType())
                .targetId(po.getTargetId())
                .targetName(po.getTargetName())
                .levelNum(po.getLevelNum())
                .levelName(po.getLevelName())
                .rankNo(po.getRankNo())
                .score(po.getScore())
                .conditionDetails(po.getConditionDetails())
                .upgradeHint(po.getUpgradeHint())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
