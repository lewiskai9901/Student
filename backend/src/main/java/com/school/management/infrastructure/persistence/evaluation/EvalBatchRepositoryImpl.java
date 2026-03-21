package com.school.management.infrastructure.persistence.evaluation;

import com.school.management.domain.evaluation.model.EvalBatch;
import com.school.management.domain.evaluation.repository.EvalBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 评选执行批次仓储实现
 */
@Repository
@RequiredArgsConstructor
public class EvalBatchRepositoryImpl implements EvalBatchRepository {

    private final EvalBatchMapper batchMapper;

    @Override
    public EvalBatch save(EvalBatch batch) {
        EvalBatchPO po = toPO(batch);
        if (batch.getId() == null) {
            batchMapper.insert(po);
            batch.setId(po.getId());
        } else {
            batchMapper.updateById(po);
        }
        return batch;
    }

    @Override
    public Optional<EvalBatch> findById(Long id) {
        EvalBatchPO po = batchMapper.selectById(id);
        if (po == null) return Optional.empty();
        return Optional.of(toDomain(po));
    }

    @Override
    public List<EvalBatch> findByCampaignId(Long campaignId) {
        return batchMapper.findByCampaignId(campaignId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    private EvalBatchPO toPO(EvalBatch d) {
        EvalBatchPO po = new EvalBatchPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setCampaignId(d.getCampaignId());
        po.setCycleStart(d.getCycleStart());
        po.setCycleEnd(d.getCycleEnd());
        po.setTotalTargets(d.getTotalTargets());
        po.setExecutedAt(d.getExecutedAt());
        po.setExecutedBy(d.getExecutedBy());
        po.setStatus(d.getStatus());
        po.setSummary(d.getSummary());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private EvalBatch toDomain(EvalBatchPO po) {
        return EvalBatch.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .campaignId(po.getCampaignId())
                .cycleStart(po.getCycleStart())
                .cycleEnd(po.getCycleEnd())
                .totalTargets(po.getTotalTargets())
                .executedAt(po.getExecutedAt())
                .executedBy(po.getExecutedBy())
                .status(po.getStatus())
                .summary(po.getSummary())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
