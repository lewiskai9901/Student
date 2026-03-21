package com.school.management.application.evaluation;

import com.school.management.domain.evaluation.engine.EvaluationEngine;
import com.school.management.domain.evaluation.model.*;
import com.school.management.domain.evaluation.repository.EvalBatchRepository;
import com.school.management.domain.evaluation.repository.EvalCampaignRepository;
import com.school.management.infrastructure.persistence.evaluation.EvalLevelRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 评选活动应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalCampaignApplicationService {

    private final EvalCampaignRepository campaignRepository;
    private final EvalBatchRepository batchRepository;
    private final EvalLevelRepositoryImpl levelRepository;
    private final EvaluationEngine evaluationEngine;

    // ==================== CRUD ====================

    @Transactional
    public EvalCampaign createCampaign(String campaignName, String targetType,
                                        String description, String evaluationPeriod,
                                        String scopeOrgIds, Long createdBy) {
        EvalCampaign campaign = EvalCampaign.builder()
                .campaignName(campaignName)
                .campaignDescription(description)
                .targetType(targetType)
                .evaluationPeriod(evaluationPeriod != null ? evaluationPeriod : "MONTHLY")
                .scopeOrgIds(scopeOrgIds)
                .status("DRAFT")
                .isAutoExecute(false)
                .sortOrder(0)
                .tenantId(0L)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return campaignRepository.save(campaign);
    }

    @Transactional
    public EvalCampaign updateCampaign(Long id, String campaignName, String description,
                                        String evaluationPeriod, String scopeOrgIds,
                                        String status, Boolean isAutoExecute, Long updatedBy) {
        EvalCampaign existing = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评选活动不存在: " + id));

        EvalCampaign updated = EvalCampaign.builder()
                .id(existing.getId())
                .tenantId(existing.getTenantId())
                .campaignName(campaignName != null ? campaignName : existing.getCampaignName())
                .campaignDescription(description != null ? description : existing.getCampaignDescription())
                .targetType(existing.getTargetType())
                .evaluationPeriod(evaluationPeriod != null ? evaluationPeriod : existing.getEvaluationPeriod())
                .scopeOrgIds(scopeOrgIds != null ? scopeOrgIds : existing.getScopeOrgIds())
                .status(status != null ? status : existing.getStatus())
                .isAutoExecute(isAutoExecute != null ? isAutoExecute : existing.getIsAutoExecute())
                .sortOrder(existing.getSortOrder())
                .createdBy(existing.getCreatedBy())
                .updatedBy(updatedBy)
                .createdAt(existing.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .lastExecutedAt(existing.getLastExecutedAt())
                .nextExecuteAt(existing.getNextExecuteAt())
                .build();
        return campaignRepository.save(updated);
    }

    @Transactional
    public void deleteCampaign(Long id) {
        campaignRepository.deleteById(id);
    }

    public Optional<EvalCampaign> getCampaign(Long id) {
        return campaignRepository.findByIdWithLevels(id);
    }

    public List<EvalCampaign> listCampaigns(String status) {
        if (status != null && !status.isBlank()) {
            return campaignRepository.findByStatus(status);
        }
        return campaignRepository.findAll();
    }

    // ==================== Levels ====================

    @Transactional
    public void saveLevels(Long campaignId, List<EvalLevel> levels) {
        // 验证活动存在
        campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("评选活动不存在: " + campaignId));
        levelRepository.replaceLevels(campaignId, levels);
    }

    public Optional<EvalCampaign> getLevels(Long campaignId) {
        return campaignRepository.findByIdWithLevels(campaignId);
    }

    // ==================== Execute ====================

    @Transactional
    public EvalBatch executeCampaign(Long campaignId, LocalDate cycleStart, LocalDate cycleEnd, Long userId) {
        EvalBatch batch = evaluationEngine.execute(campaignId, cycleStart, cycleEnd, userId);

        // 更新活动的 lastExecutedAt
        campaignRepository.findById(campaignId).ifPresent(c -> {
            EvalCampaign updated = EvalCampaign.builder()
                    .id(c.getId())
                    .tenantId(c.getTenantId())
                    .campaignName(c.getCampaignName())
                    .campaignDescription(c.getCampaignDescription())
                    .targetType(c.getTargetType())
                    .evaluationPeriod(c.getEvaluationPeriod())
                    .scopeOrgIds(c.getScopeOrgIds())
                    .status(c.getStatus())
                    .isAutoExecute(c.getIsAutoExecute())
                    .sortOrder(c.getSortOrder())
                    .createdBy(c.getCreatedBy())
                    .updatedBy(userId)
                    .createdAt(c.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .lastExecutedAt(LocalDateTime.now())
                    .nextExecuteAt(c.getNextExecuteAt())
                    .build();
            campaignRepository.save(updated);
        });

        return batch;
    }

    public List<EvalBatch> listBatches(Long campaignId) {
        return batchRepository.findByCampaignId(campaignId);
    }
}
