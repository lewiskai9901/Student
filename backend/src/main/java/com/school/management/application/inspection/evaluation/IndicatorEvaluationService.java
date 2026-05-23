package com.school.management.application.inspection.evaluation;

import com.school.management.domain.inspection.model.evaluation.IndicatorResult;
import com.school.management.domain.inspection.model.evaluation.ResultStatus;
import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import com.school.management.domain.inspection.repository.IndicatorResultRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 评级编排服务 (Phase 3, 2026-05-23).
 *
 * <p>职责:
 * <ol>
 *   <li>调 {@link IndicatorComputeEngine} 算出 DRAFT</li>
 *   <li>查同 (indicator, target, periodKey) 是否已有 PUBLISHED →
 *       有则走 {@code supersedeWith()} 链 (实现 LatePolicy.REVISE_ORIGINAL);
 *       无则直接落 DRAFT</li>
 *   <li>注入 tenantId / orgUnitId (从 indicator.project 反查)</li>
 * </ol>
 *
 * <p>注: 不自动 publish. 业务方决定何时 publish (可立即 / 可审核后).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorEvaluationService {

    private final IndicatorComputeEngine computeEngine;
    private final IndicatorRepository indicatorRepository;
    private final IndicatorResultRepository resultRepository;
    private final InspProjectRepository projectRepository;

    /**
     * 评估某 indicator 在某周期: 算 + 持久化 DRAFT (或 supersede 链).
     *
     * @return 新落库的 DRAFT 结果 (不含被 SUPERSEDED 的旧)
     */
    @Transactional
    public List<IndicatorResult> evaluate(Long indicatorId, String periodKey,
                                          LocalDate periodStart, LocalDate periodEnd) {
        Indicator indicator = indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new IllegalArgumentException("Indicator 不存在: " + indicatorId));

        // 项目级 orgUnitId / tenantId 反查 (用于结果数据权限)
        Long orgUnitId = null;
        Long tenantId = indicator.getTenantId();
        if (indicator.getProjectId() != null) {
            Optional<InspProject> proj = projectRepository.findById(indicator.getProjectId());
            if (proj.isPresent()) {
                if (orgUnitId == null) orgUnitId = proj.get().getOrgUnitId();
                if (tenantId == null) tenantId = proj.get().getTenantId();
            }
        }

        List<IndicatorResult> drafts = computeEngine.compute(indicator, periodKey, periodStart, periodEnd);
        if (drafts.isEmpty()) {
            log.debug("Indicator {} period {} 无结果", indicatorId, periodKey);
            return List.of();
        }

        List<IndicatorResult> persisted = new ArrayList<>(drafts.size());
        for (IndicatorResult fresh : drafts) {
            fresh.setOrgUnitId(orgUnitId);
            fresh.setTenantId(tenantId);

            Optional<IndicatorResult> existingPublished = resultRepository
                    .findCurrentPublished(indicatorId, fresh.getTargetId(), periodKey);

            if (existingPublished.isPresent()) {
                IndicatorResult prev = existingPublished.get();
                // 值一致则无需 supersede (避免噪声链)
                if (sameValue(prev, fresh)) {
                    log.debug("Indicator {} target {} 值未变, 跳过 supersede", indicatorId, fresh.getTargetId());
                    continue;
                }
                // supersede 链: 旧→SUPERSEDED + 新 DRAFT(revisionOf=旧.id)
                IndicatorResult newDraft = prev.supersedeWith(
                        fresh.getValue(),
                        fresh.getRankPosition(),
                        fresh.getGrade(),
                        fresh.getSourceSubmissionIds(),
                        fresh.getSourceSectionIds());
                newDraft.setOrgUnitId(orgUnitId);
                newDraft.setTenantId(tenantId);
                resultRepository.save(prev);          // 落 SUPERSEDED
                persisted.add(resultRepository.save(newDraft));
            } else {
                persisted.add(resultRepository.save(fresh));
            }
        }
        log.info("Indicator {} period {} 评估: {} 新 DRAFT", indicatorId, periodKey, persisted.size());
        return persisted;
    }

    /** Manual trigger 入口: periodKey=MANUAL:start_end. */
    @Transactional
    public List<IndicatorResult> evaluateManual(Long indicatorId, LocalDate start, LocalDate end) {
        String key = PeriodKeyResolver.forManual(start, end);
        return evaluate(indicatorId, key, start, end);
    }

    /** 单条 DRAFT → PUBLISHED. */
    @Transactional
    public IndicatorResult publish(Long resultId) {
        IndicatorResult r = resultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("IndicatorResult 不存在: " + resultId));
        r.publish();
        return resultRepository.save(r);
    }

    /** 批量发布 indicator+periodKey 下所有 DRAFT. */
    @Transactional
    public int publishAllDrafts(Long indicatorId, String periodKey) {
        int n = 0;
        List<IndicatorResult> drafts = resultRepository
                .findByIndicatorIdAndStatus(indicatorId, ResultStatus.DRAFT);
        for (IndicatorResult r : drafts) {
            if (!periodKey.equals(r.getPeriodKey())) continue;
            r.publish();
            resultRepository.save(r);
            n++;
        }
        log.info("Indicator {} period {} 批量发布 {} 个 DRAFT", indicatorId, periodKey, n);
        return n;
    }

    private boolean sameValue(IndicatorResult a, IndicatorResult b) {
        if (a.getValue() == null || b.getValue() == null) return false;
        return a.getValue().compareTo(b.getValue()) == 0;
    }
}
