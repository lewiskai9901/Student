package com.school.management.application.inspection.evaluation;

import com.school.management.domain.inspection.event.SubmissionCompletedEvent;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionStatus;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.TriggerMode;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CountThresholdTrigger 单测")
class CountThresholdTriggerTest {

    @Mock IndicatorRepository indicatorRepository;
    @Mock InspTaskRepository taskRepository;
    @Mock InspSubmissionRepository submissionRepository;
    @Mock IndicatorEvaluationService evaluationService;

    @InjectMocks CountThresholdTrigger trigger;

    private Indicator countIndicator(long id, int threshold) {
        return Indicator.builder()
                .id(id).projectId(1L).name("c" + id)
                .indicatorType("LEAF").sourceSectionIds(List.of(10L))
                .triggerMode(TriggerMode.COUNT).countThreshold(threshold)
                .build();
    }

    private InspSubmission completedSub(long id, long taskId, long sectionId, long targetId) {
        return InspSubmission.builder()
                .id(id).taskId(taskId).sectionId(sectionId)
                .targetType(TargetType.USER).targetId(targetId).targetName("T")
                .status(SubmissionStatus.COMPLETED).finalScore(BigDecimal.valueOf(80))
                .build();
    }

    @Test
    @DisplayName("count % threshold == 0 → 触发, periodKey=COUNT#bucket")
    void triggersOnExactMultiple() {
        // 触发事件: submission id=5, task=200, section=10, target=555
        InspSubmission trig = completedSub(5L, 200L, 10L, 555L);
        when(submissionRepository.findById(5L)).thenReturn(Optional.of(trig));

        InspTask task = InspTask.builder().id(200L).projectId(1L).taskDate(LocalDate.now()).build();
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));

        Indicator ind = countIndicator(99L, 3);
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(ind));

        // count = 3 (恰好达阈值)
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(task));
        when(submissionRepository.findByTaskId(200L)).thenReturn(List.of(
                completedSub(1L, 200L, 10L, 555L),
                completedSub(2L, 200L, 10L, 555L),
                completedSub(3L, 200L, 10L, 555L)
        ));

        trigger.onSubmissionCompleted(new SubmissionCompletedEvent(5L, 200L, "USER", 555L, BigDecimal.valueOf(80)));

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(evaluationService).evaluate(eq(99L), keyCap.capture(), any(), any());
        assertThat(keyCap.getValue()).isEqualTo("COUNT#1");
    }

    @Test
    @DisplayName("count % threshold != 0 → 不触发")
    void noTriggerWhenBelowThreshold() {
        InspSubmission trig = completedSub(5L, 200L, 10L, 555L);
        when(submissionRepository.findById(5L)).thenReturn(Optional.of(trig));
        InspTask task = InspTask.builder().id(200L).projectId(1L).taskDate(LocalDate.now()).build();
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));

        Indicator ind = countIndicator(99L, 3);
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(ind));

        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(task));
        when(submissionRepository.findByTaskId(200L)).thenReturn(List.of(
                completedSub(1L, 200L, 10L, 555L),
                completedSub(2L, 200L, 10L, 555L)
        ));

        trigger.onSubmissionCompleted(new SubmissionCompletedEvent(5L, 200L, "USER", 555L, BigDecimal.valueOf(80)));

        verify(evaluationService, never()).evaluate(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("非 COUNT 模式 indicator → 不触发")
    void skipsNonCountMode() {
        InspSubmission trig = completedSub(5L, 200L, 10L, 555L);
        when(submissionRepository.findById(5L)).thenReturn(Optional.of(trig));
        InspTask task = InspTask.builder().id(200L).projectId(1L).taskDate(LocalDate.now()).build();
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));

        Indicator twi = Indicator.builder().id(100L).projectId(1L)
                .indicatorType("LEAF").sourceSectionIds(List.of(10L))
                .triggerMode(TriggerMode.TIME_WINDOW).build();
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(twi));

        trigger.onSubmissionCompleted(new SubmissionCompletedEvent(5L, 200L, "USER", 555L, BigDecimal.valueOf(80)));

        verify(evaluationService, never()).evaluate(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("section 不匹配 → 不触发")
    void skipsWhenSectionMismatch() {
        InspSubmission trig = completedSub(5L, 200L, 99L /* 不在 indicator section */, 555L);
        when(submissionRepository.findById(5L)).thenReturn(Optional.of(trig));
        InspTask task = InspTask.builder().id(200L).projectId(1L).taskDate(LocalDate.now()).build();
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));

        Indicator ind = countIndicator(99L, 1);
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(ind));

        trigger.onSubmissionCompleted(new SubmissionCompletedEvent(5L, 200L, "USER", 555L, BigDecimal.valueOf(80)));

        verify(evaluationService, never()).evaluate(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("submission 不存在 → 安全跳过")
    void missingSubmissionSafe() {
        when(submissionRepository.findById(5L)).thenReturn(Optional.empty());

        trigger.onSubmissionCompleted(new SubmissionCompletedEvent(5L, 200L, "USER", 555L, BigDecimal.valueOf(80)));

        verify(evaluationService, never()).evaluate(any(), anyString(), any(), any());
    }
}
