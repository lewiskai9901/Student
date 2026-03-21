package com.school.management.infrastructure.persistence.evaluation;

import com.school.management.domain.evaluation.model.EvalCondition;
import com.school.management.domain.evaluation.model.EvalLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评选级别 + 条件的保存辅助（非独立仓储接口，由 ApplicationService 直接使用）
 */
@Repository
@RequiredArgsConstructor
public class EvalLevelRepositoryImpl {

    private final EvalLevelMapper levelMapper;
    private final EvalConditionMapper conditionMapper;

    /**
     * 删除活动下所有级别和条件，再重新插入
     */
    public void replaceLevels(Long campaignId, List<EvalLevel> levels) {
        // 先删条件再删级别（避免子查询 DELETE 限制）
        conditionMapper.deleteByCampaignId(campaignId);
        levelMapper.deleteByCampaignId(campaignId);

        for (EvalLevel level : levels) {
            EvalLevelPO po = toLevelPO(campaignId, level);
            levelMapper.insert(po);
            level.setId(po.getId());

            if (level.getConditions() != null) {
                for (EvalCondition condition : level.getConditions()) {
                    EvalConditionPO cpo = toConditionPO(po.getId(), condition);
                    conditionMapper.insert(cpo);
                    condition.setId(cpo.getId());
                }
            }
        }
    }

    private EvalLevelPO toLevelPO(Long campaignId, EvalLevel d) {
        EvalLevelPO po = new EvalLevelPO();
        po.setCampaignId(campaignId);
        po.setLevelNum(d.getLevelNum());
        po.setLevelName(d.getLevelName());
        po.setConditionLogic(d.getConditionLogic() != null ? d.getConditionLogic() : "AND");
        po.setSortOrder(d.getSortOrder() != null ? d.getSortOrder() : 0);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        return po;
    }

    private EvalConditionPO toConditionPO(Long levelId, EvalCondition d) {
        EvalConditionPO po = new EvalConditionPO();
        po.setLevelId(levelId);
        po.setSourceType(d.getSourceType());
        po.setSourceConfig(d.getSourceConfig());
        po.setMetric(d.getMetric());
        po.setOperator(d.getOperator());
        po.setThreshold(d.getThreshold());
        po.setScope(d.getScope() != null ? d.getScope() : "SELF");
        po.setScopeRole(d.getScopeRole());
        po.setTimeRange(d.getTimeRange() != null ? d.getTimeRange() : "CYCLE");
        po.setTimeRangeDays(d.getTimeRangeDays());
        po.setDescription(d.getDescription());
        po.setSortOrder(d.getSortOrder() != null ? d.getSortOrder() : 0);
        po.setCreatedAt(LocalDateTime.now());
        return po;
    }
}
