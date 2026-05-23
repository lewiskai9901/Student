package com.school.management.application.inspection.evaluation;

import com.school.management.domain.inspection.model.evaluation.IndicatorResult;
import com.school.management.domain.inspection.model.evaluation.ResultStatus;
import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import com.school.management.domain.inspection.repository.IndicatorResultRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndicatorEvaluationService 单测")
class IndicatorEvaluationServiceTest {

    @Mock IndicatorComputeEngine computeEngine;
    @Mock IndicatorRepository indicatorRepository;
    @Mock IndicatorResultRepository resultRepository;
    @Mock InspProjectRepository projectRepository;

    @InjectMocks IndicatorEvaluationService service;

    private Indicator anIndicator() {
        return Indicator.builder().id(1L).projectId(100L).name("X")
                .indicatorType("LEAF").sourceSectionIds(List.of(10L)).build();
    }

    private IndicatorResult draftFor(Long indId, Long targetId, String key, double value) {
        return IndicatorResult.draft(indId, targetId, "T" + targetId, key,
                BigDecimal.valueOf(value), 1, "A",
                List.of(1L), List.of(10L));
    }

    @Test
    @DisplayName("无既有 PUBLISHED → 直接落 DRAFT")
    void evaluateFreshDraft() {
        Indicator ind = anIndicator();
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(ind));
        when(projectRepository.findById(100L)).thenReturn(Optional.empty());

        IndicatorResult fresh = draftFor(1L, 555L, "2026-W21", 90.0);
        when(computeEngine.compute(eq(ind), eq("2026-W21"), any(), any())).thenReturn(List.of(fresh));
        when(resultRepository.findCurrentPublished(1L, 555L, "2026-W21")).thenReturn(Optional.empty());
        when(resultRepository.save(any(IndicatorResult.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<IndicatorResult> out = service.evaluate(1L, "2026-W21",
                LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 24));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getStatus()).isEqualTo(ResultStatus.DRAFT);
        verify(resultRepository).save(any(IndicatorResult.class)); // 仅 1 次 save
    }

    @Test
    @DisplayName("已有 PUBLISHED 且新值不同 → supersede 链 (旧 SUPERSEDED + 新 DRAFT)")
    void supersedeChain() {
        Indicator ind = anIndicator();
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(ind));
        when(projectRepository.findById(100L)).thenReturn(Optional.empty());

        // 既有 PUBLISHED (值=80)
        IndicatorResult prev = IndicatorResult.builder()
                .id(900L)
                .indicatorId(1L).targetId(555L).targetName("T")
                .periodKey("2026-W21").value(BigDecimal.valueOf(80)).rankPosition(2).grade("B")
                .status(ResultStatus.PUBLISHED)
                .build();

        // 新 DRAFT (值=95)
        IndicatorResult fresh = draftFor(1L, 555L, "2026-W21", 95.0);
        when(computeEngine.compute(eq(ind), eq("2026-W21"), any(), any())).thenReturn(List.of(fresh));
        when(resultRepository.findCurrentPublished(1L, 555L, "2026-W21")).thenReturn(Optional.of(prev));
        when(resultRepository.save(any(IndicatorResult.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<IndicatorResult> out = service.evaluate(1L, "2026-W21",
                LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 24));

        // 旧 prev 应当被改为 SUPERSEDED, 新 DRAFT revisionOf=prev.id
        assertThat(prev.getStatus()).isEqualTo(ResultStatus.SUPERSEDED);
        assertThat(out).hasSize(1);
        IndicatorResult newDraft = out.get(0);
        assertThat(newDraft.getStatus()).isEqualTo(ResultStatus.DRAFT);
        assertThat(newDraft.getRevisionOf()).isEqualTo(900L);
        assertThat(newDraft.getValue()).isEqualByComparingTo(BigDecimal.valueOf(95));

        // save 至少 2 次 (旧 + 新)
        verify(resultRepository, atLeast(2)).save(any(IndicatorResult.class));
    }

    @Test
    @DisplayName("已有 PUBLISHED 且新值相同 → 跳过 supersede, 不产生噪声")
    void sameValueSkip() {
        Indicator ind = anIndicator();
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(ind));
        when(projectRepository.findById(100L)).thenReturn(Optional.empty());

        IndicatorResult prev = IndicatorResult.builder()
                .id(901L).indicatorId(1L).targetId(555L).targetName("T")
                .periodKey("2026-W21").value(BigDecimal.valueOf(90)).status(ResultStatus.PUBLISHED).build();

        IndicatorResult fresh = draftFor(1L, 555L, "2026-W21", 90.0);
        when(computeEngine.compute(eq(ind), eq("2026-W21"), any(), any())).thenReturn(List.of(fresh));
        when(resultRepository.findCurrentPublished(1L, 555L, "2026-W21")).thenReturn(Optional.of(prev));

        List<IndicatorResult> out = service.evaluate(1L, "2026-W21",
                LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 24));

        assertThat(out).isEmpty();
        assertThat(prev.getStatus()).isEqualTo(ResultStatus.PUBLISHED); // 未变
        verify(resultRepository, never()).save(any(IndicatorResult.class));
    }

    @Test
    @DisplayName("publish: DRAFT → PUBLISHED")
    void publishDraft() {
        IndicatorResult r = draftFor(1L, 555L, "X", 90.0);
        r.setOrgUnitId(7L);
        // 用 reconstruct 给 id
        IndicatorResult reconstructed = IndicatorResult.builder()
                .id(123L).indicatorId(1L).targetId(555L).targetName("T")
                .periodKey("X").value(BigDecimal.valueOf(90)).status(ResultStatus.DRAFT)
                .build();
        when(resultRepository.findById(123L)).thenReturn(Optional.of(reconstructed));
        when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IndicatorResult published = service.publish(123L);
        assertThat(published.getStatus()).isEqualTo(ResultStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("evaluateManual 走 MANUAL: periodKey")
    void manualKey() {
        Indicator ind = anIndicator();
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(ind));
        when(projectRepository.findById(100L)).thenReturn(Optional.empty());
        when(computeEngine.compute(eq(ind), anyString(), any(), any())).thenReturn(List.of());

        service.evaluateManual(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(computeEngine).compute(eq(ind), keyCap.capture(), any(), any());
        assertThat(keyCap.getValue()).isEqualTo("MANUAL:2026-05-01_2026-05-31");
    }

    @Test
    @DisplayName("orgUnitId/tenantId 从 project 反查注入到结果")
    void orgUnitInjection() {
        Indicator ind = anIndicator();
        InspProject proj = InspProject.builder().id(100L).orgUnitId(42L).tenantId(7L).build();
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(ind));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(proj));

        IndicatorResult fresh = draftFor(1L, 555L, "K", 60.0);
        when(computeEngine.compute(eq(ind), eq("K"), any(), any())).thenReturn(List.of(fresh));
        when(resultRepository.findCurrentPublished(anyLong(), anyLong(), anyString())).thenReturn(Optional.empty());
        when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<IndicatorResult> out = service.evaluate(1L, "K", LocalDate.now(), LocalDate.now());
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getOrgUnitId()).isEqualTo(42L);
        assertThat(out.get(0).getTenantId()).isEqualTo(7L);
    }
}
