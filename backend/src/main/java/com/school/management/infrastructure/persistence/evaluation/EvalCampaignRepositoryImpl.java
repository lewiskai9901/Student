package com.school.management.infrastructure.persistence.evaluation;

import com.school.management.domain.evaluation.model.EvalCampaign;
import com.school.management.domain.evaluation.model.EvalCondition;
import com.school.management.domain.evaluation.model.EvalLevel;
import com.school.management.domain.evaluation.repository.EvalCampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 评选活动仓储实现
 */
@Repository
@RequiredArgsConstructor
public class EvalCampaignRepositoryImpl implements EvalCampaignRepository {

    private final EvalCampaignMapper campaignMapper;
    private final EvalLevelMapper levelMapper;
    private final EvalConditionMapper conditionMapper;

    @Override
    public EvalCampaign save(EvalCampaign campaign) {
        EvalCampaignPO po = toPO(campaign);
        if (campaign.getId() == null) {
            campaignMapper.insert(po);
            campaign.setId(po.getId());
        } else {
            campaignMapper.updateById(po);
        }
        return campaign;
    }

    @Override
    public Optional<EvalCampaign> findById(Long id) {
        EvalCampaignPO po = campaignMapper.selectById(id);
        if (po == null) return Optional.empty();
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<EvalCampaign> findByIdWithLevels(Long id) {
        EvalCampaignPO po = campaignMapper.selectById(id);
        if (po == null) return Optional.empty();

        EvalCampaign campaign = toDomain(po);

        List<EvalLevelPO> levelPOs = levelMapper.findByCampaignId(id);
        List<EvalLevel> levels = levelPOs.stream().map(lpo -> {
            List<EvalCondition> conditions = conditionMapper.findByLevelId(lpo.getId())
                    .stream().map(this::toConditionDomain).collect(Collectors.toList());
            EvalLevel level = toLevelDomain(lpo);
            level.setConditions(conditions);
            return level;
        }).collect(Collectors.toList());

        campaign.setLevels(levels);
        return Optional.of(campaign);
    }

    @Override
    public List<EvalCampaign> findAll() {
        return campaignMapper.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EvalCampaign> findByStatus(String status) {
        return campaignMapper.findByStatus(status).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        campaignMapper.deleteById(id);
    }

    // ========== Mapping ===========

    private EvalCampaignPO toPO(EvalCampaign d) {
        EvalCampaignPO po = new EvalCampaignPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setCampaignName(d.getCampaignName());
        po.setCampaignDescription(d.getCampaignDescription());
        po.setTargetType(d.getTargetType());
        po.setScopeOrgIds(d.getScopeOrgIds());
        po.setEvaluationPeriod(d.getEvaluationPeriod());
        po.setStatus(d.getStatus());
        po.setIsAutoExecute(d.getIsAutoExecute());
        po.setLastExecutedAt(d.getLastExecutedAt());
        po.setNextExecuteAt(d.getNextExecuteAt());
        po.setSortOrder(d.getSortOrder());
        po.setCreatedBy(d.getCreatedBy());
        po.setUpdatedBy(d.getUpdatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private EvalCampaign toDomain(EvalCampaignPO po) {
        return EvalCampaign.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .campaignName(po.getCampaignName())
                .campaignDescription(po.getCampaignDescription())
                .targetType(po.getTargetType())
                .scopeOrgIds(po.getScopeOrgIds())
                .evaluationPeriod(po.getEvaluationPeriod())
                .status(po.getStatus())
                .isAutoExecute(po.getIsAutoExecute())
                .lastExecutedAt(po.getLastExecutedAt())
                .nextExecuteAt(po.getNextExecuteAt())
                .sortOrder(po.getSortOrder())
                .createdBy(po.getCreatedBy())
                .updatedBy(po.getUpdatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private EvalLevel toLevelDomain(EvalLevelPO po) {
        return EvalLevel.builder()
                .id(po.getId())
                .campaignId(po.getCampaignId())
                .levelNum(po.getLevelNum())
                .levelName(po.getLevelName())
                .conditionLogic(po.getConditionLogic())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private EvalCondition toConditionDomain(EvalConditionPO po) {
        return EvalCondition.builder()
                .id(po.getId())
                .levelId(po.getLevelId())
                .sourceType(po.getSourceType())
                .sourceConfig(po.getSourceConfig())
                .metric(po.getMetric())
                .operator(po.getOperator())
                .threshold(po.getThreshold())
                .scope(po.getScope())
                .scopeRole(po.getScopeRole())
                .timeRange(po.getTimeRange())
                .timeRangeDays(po.getTimeRangeDays())
                .description(po.getDescription())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
