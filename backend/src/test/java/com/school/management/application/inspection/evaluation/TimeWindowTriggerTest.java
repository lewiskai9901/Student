package com.school.management.application.inspection.evaluation;

import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.ProjectStatus;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.TriggerMode;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeWindowTrigger 单测")
class TimeWindowTriggerTest {

    @Mock IndicatorRepository indicatorRepository;
    @Mock InspProjectRepository projectRepository;
    @Mock IndicatorEvaluationService evaluationService;

    @InjectMocks TimeWindowTrigger trigger;

    private Indicator indicator(long id, TriggerMode mode, String period) {
        return Indicator.builder()
                .id(id).projectId(1L).name("ind" + id)
                .indicatorType("LEAF").sourceSectionIds(List.of(10L))
                .triggerMode(mode).evaluationPeriod(period)
                .build();
    }

    @Test
    @DisplayName("DAILY 扫: 仅触发 TIME_WINDOW+DAILY, periodKey=yyyy-MM-dd")
    void dailyScan() {
        InspProject proj = InspProject.builder().id(1L).build();
        when(projectRepository.findByStatus(ProjectStatus.PUBLISHED)).thenReturn(List.of(proj));
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(
                indicator(1L, TriggerMode.TIME_WINDOW, "DAILY"),
                indicator(2L, TriggerMode.TIME_WINDOW, "WEEKLY"), // 不匹配
                indicator(3L, TriggerMode.COUNT, "DAILY"),         // 不匹配
                indicator(4L, TriggerMode.MANUAL, "DAILY")         // 不匹配
        ));

        LocalDate ref = LocalDate.of(2026, 5, 23);
        int count = trigger.runForPeriod("DAILY", ref);

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> startCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCap = ArgumentCaptor.forClass(LocalDate.class);
        verify(evaluationService).evaluate(eq(1L), keyCap.capture(), startCap.capture(), endCap.capture());
        assertThat(keyCap.getValue()).isEqualTo("2026-05-23");
        assertThat(startCap.getValue()).isEqualTo(ref);
        assertThat(endCap.getValue()).isEqualTo(ref);
    }

    @Test
    @DisplayName("WEEKLY 扫: 边界=ISO 周 (周一-周日), periodKey=yyyy-Www")
    void weeklyScan() {
        InspProject proj = InspProject.builder().id(1L).build();
        when(projectRepository.findByStatus(ProjectStatus.PUBLISHED)).thenReturn(List.of(proj));
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(
                indicator(10L, TriggerMode.TIME_WINDOW, "WEEKLY")
        ));

        // 2026-05-22 是周五, 所属 ISO 周 = W21 (周一=2026-05-18)
        LocalDate ref = LocalDate.of(2026, 5, 22);
        trigger.runForPeriod("WEEKLY", ref);

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> startCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCap = ArgumentCaptor.forClass(LocalDate.class);
        verify(evaluationService).evaluate(eq(10L), keyCap.capture(), startCap.capture(), endCap.capture());
        assertThat(keyCap.getValue()).isEqualTo("2026-W21");
        assertThat(startCap.getValue()).isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(endCap.getValue()).isEqualTo(LocalDate.of(2026, 5, 24));
    }

    @Test
    @DisplayName("MONTHLY 扫: 边界=月头-月末, periodKey=yyyy-MM")
    void monthlyScan() {
        InspProject proj = InspProject.builder().id(1L).build();
        when(projectRepository.findByStatus(ProjectStatus.PUBLISHED)).thenReturn(List.of(proj));
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(
                indicator(20L, TriggerMode.TIME_WINDOW, "MONTHLY")
        ));

        LocalDate ref = LocalDate.of(2026, 5, 1);
        trigger.runForPeriod("MONTHLY", ref);

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> startCap = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCap = ArgumentCaptor.forClass(LocalDate.class);
        verify(evaluationService).evaluate(eq(20L), keyCap.capture(), startCap.capture(), endCap.capture());
        assertThat(keyCap.getValue()).isEqualTo("2026-05");
        assertThat(startCap.getValue()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(endCap.getValue()).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    @DisplayName("单个 indicator 失败不影响其他 indicator")
    void failureIsolation() {
        InspProject proj = InspProject.builder().id(1L).build();
        when(projectRepository.findByStatus(ProjectStatus.PUBLISHED)).thenReturn(List.of(proj));
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(
                indicator(1L, TriggerMode.TIME_WINDOW, "DAILY"),
                indicator(2L, TriggerMode.TIME_WINDOW, "DAILY")
        ));
        when(evaluationService.evaluate(eq(1L), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        int count = trigger.runForPeriod("DAILY", LocalDate.of(2026, 5, 23));

        assertThat(count).isEqualTo(1); // 仅 2 号成功
        verify(evaluationService).evaluate(eq(2L), any(), any(), any());
    }
}
