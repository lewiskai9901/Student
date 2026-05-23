package com.school.management.application.inspection.evaluation;

import com.school.management.domain.inspection.model.evaluation.IndicatorResult;
import com.school.management.domain.inspection.model.evaluation.ResultStatus;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionStatus;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.model.scoring.GradeDefinition;
import com.school.management.domain.inspection.model.scoring.GradeScheme;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.MissingPolicy;
import com.school.management.domain.inspection.model.scoring.RankDirection;
import com.school.management.domain.inspection.repository.GradeDefinitionRepository;
import com.school.management.domain.inspection.repository.GradeSchemeRepository;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndicatorComputeEngine 单测")
class IndicatorComputeEngineTest {

    @Mock InspTaskRepository taskRepository;
    @Mock InspSubmissionRepository submissionRepository;
    @Mock GradeSchemeRepository gradeSchemeRepository;
    @Mock GradeDefinitionRepository gradeDefinitionRepository;

    @InjectMocks IndicatorComputeEngine engine;

    private final LocalDate D1 = LocalDate.of(2026, 5, 18); // Monday
    private final LocalDate D2 = LocalDate.of(2026, 5, 24); // Sunday

    private GradeScheme defaultScheme;

    @BeforeEach
    void setupScheme() {
        defaultScheme = GradeScheme.builder().id(1L).displayName("默认").build();
        defaultScheme.setGrades(List.of(
                GradeDefinition.builder().code("A").minValue(BigDecimal.valueOf(90))
                        .maxValue(BigDecimal.valueOf(100)).build(),
                GradeDefinition.builder().code("B").minValue(BigDecimal.valueOf(80))
                        .maxValue(BigDecimal.valueOf(89)).build(),
                GradeDefinition.builder().code("C").minValue(BigDecimal.valueOf(0))
                        .maxValue(BigDecimal.valueOf(79)).build()
        ));
        lenient().when(gradeSchemeRepository.findById(1L)).thenReturn(Optional.of(defaultScheme));
        lenient().when(gradeDefinitionRepository.findByGradeSchemeId(1L))
                .thenReturn(defaultScheme.getGrades());
    }

    private Indicator newIndicator(List<Long> sectionIds, MissingPolicy policy, RankDirection rank) {
        return Indicator.builder()
                .id(101L).projectId(1L).name("测试")
                .indicatorType("LEAF")
                .sourceSectionIds(sectionIds)
                .sourceAggregation("AVG")
                .missingPolicy(policy)
                .rankDirection(rank)
                .evaluationPeriod("WEEKLY")
                .gradeSchemeId(1L)
                .evaluationMethod("SCORE_RANGE")
                .gradeThresholds("[{\"gradeCode\":\"A\",\"value\":90},{\"gradeCode\":\"B\",\"value\":80},{\"gradeCode\":\"C\",\"value\":0}]")
                .build();
    }

    private InspTask task(long id, LocalDate date) {
        return InspTask.builder().id(id).projectId(1L).taskDate(date).build();
    }

    private InspSubmission completedSub(long id, long taskId, long sectionId,
                                         long targetId, String name, double score) {
        return InspSubmission.builder()
                .id(id).taskId(taskId).sectionId(sectionId)
                .targetType(TargetType.USER).targetId(targetId).targetName(name)
                .status(SubmissionStatus.COMPLETED)
                .finalScore(BigDecimal.valueOf(score))
                .completedAt(LocalDateTime.of(2026, 5, 20, 10, 0))
                .build();
    }

    @Test
    @DisplayName("单 section 单 target AVG → 命中 SCORE_RANGE 等级")
    void singleSectionSingleTarget() {
        Indicator ind = newIndicator(List.of(10L), MissingPolicy.IGNORE, null);
        InspTask t = task(200L, D1.plusDays(2));
        when(taskRepository.findByProjectIdAndTaskDateBetween(1L, D1, D2)).thenReturn(List.of(t));
        when(submissionRepository.findByTaskId(200L)).thenReturn(List.of(
                completedSub(1L, 200L, 10L, 555L, "TargetA", 95.0),
                completedSub(2L, 200L, 10L, 555L, "TargetA", 85.0)
        ));

        List<IndicatorResult> rs = engine.compute(ind, "2026-W21", D1, D2);

        assertThat(rs).hasSize(1);
        IndicatorResult r = rs.get(0);
        assertThat(r.getStatus()).isEqualTo(ResultStatus.DRAFT);
        assertThat(r.getValue()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
        assertThat(r.getGrade()).isEqualTo("A");
        assertThat(r.getSourceSubmissionIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("跨 section weightsBySection 加权: 10->70%, 20->30%")
    void weightsBySection() {
        Indicator ind = Indicator.builder()
                .id(102L).projectId(1L).name("加权")
                .indicatorType("LEAF")
                .sourceSectionIds(List.of(10L, 20L))
                .sourceAggregation("AVG")
                .weightsBySection(Map.of(
                        10L, new BigDecimal("0.7"),
                        20L, new BigDecimal("0.3")))
                .missingPolicy(MissingPolicy.IGNORE)
                .evaluationPeriod("WEEKLY").gradeSchemeId(1L)
                .evaluationMethod("SCORE_RANGE")
                .gradeThresholds("[{\"gradeCode\":\"A\",\"value\":90},{\"gradeCode\":\"C\",\"value\":0}]")
                .build();
        InspTask t = task(300L, D1.plusDays(1));
        when(taskRepository.findByProjectIdAndTaskDateBetween(1L, D1, D2)).thenReturn(List.of(t));
        when(submissionRepository.findByTaskId(300L)).thenReturn(List.of(
                completedSub(11L, 300L, 10L, 999L, "X", 100.0),
                completedSub(12L, 300L, 20L, 999L, "X", 50.0)
        ));

        List<IndicatorResult> rs = engine.compute(ind, "2026-W21", D1, D2);

        assertThat(rs).hasSize(1);
        assertThat(rs.get(0).getValue()).isEqualByComparingTo(new BigDecimal("85.0000"));
    }

    @Test
    @DisplayName("missingPolicy=ZERO: 缺失 section 视为 0 分")
    void missingPolicyZero() {
        Indicator ind = newIndicator(List.of(10L, 20L), MissingPolicy.ZERO, null);
        ind = Indicator.builder()
                .id(103L).projectId(1L).name("Z")
                .indicatorType("LEAF")
                .sourceSectionIds(List.of(10L, 20L))
                .sourceAggregation("AVG")
                .missingPolicy(MissingPolicy.ZERO)
                .evaluationPeriod("WEEKLY").gradeSchemeId(1L)
                .evaluationMethod("SCORE_RANGE")
                .gradeThresholds("[{\"gradeCode\":\"C\",\"value\":0}]")
                .build();
        InspTask t = task(400L, D1.plusDays(2));
        when(taskRepository.findByProjectIdAndTaskDateBetween(1L, D1, D2)).thenReturn(List.of(t));
        when(submissionRepository.findByTaskId(400L)).thenReturn(List.of(
                completedSub(21L, 400L, 10L, 777L, "Y", 80.0)
                // section 20 缺
        ));

        List<IndicatorResult> rs = engine.compute(ind, "2026-W21", D1, D2);
        assertThat(rs).hasSize(1);
        // (80 + 0) / 2 = 40
        assertThat(rs.get(0).getValue()).isEqualByComparingTo(new BigDecimal("40.0000"));
    }

    @Test
    @DisplayName("missingPolicy=WAIT: 任一 section 缺失则推迟, 返回空")
    void missingPolicyWait() {
        Indicator ind = Indicator.builder()
                .id(104L).projectId(1L).name("W")
                .indicatorType("LEAF")
                .sourceSectionIds(List.of(10L, 20L))
                .missingPolicy(MissingPolicy.WAIT)
                .sourceAggregation("AVG")
                .evaluationPeriod("WEEKLY").gradeSchemeId(1L)
                .evaluationMethod("SCORE_RANGE")
                .gradeThresholds("[{\"gradeCode\":\"C\",\"value\":0}]")
                .build();
        InspTask t = task(500L, D1.plusDays(2));
        when(taskRepository.findByProjectIdAndTaskDateBetween(1L, D1, D2)).thenReturn(List.of(t));
        when(submissionRepository.findByTaskId(500L)).thenReturn(List.of(
                completedSub(31L, 500L, 10L, 777L, "Y", 80.0)
        ));

        List<IndicatorResult> rs = engine.compute(ind, "2026-W21", D1, D2);
        assertThat(rs).isEmpty();
    }

    @Test
    @DisplayName("missingPolicy=MAX: 缺失 section 视为现有最高分")
    void missingPolicyMax() {
        Indicator ind = Indicator.builder()
                .id(105L).projectId(1L).name("M")
                .indicatorType("LEAF")
                .sourceSectionIds(List.of(10L, 20L))
                .missingPolicy(MissingPolicy.MAX)
                .sourceAggregation("AVG")
                .evaluationPeriod("WEEKLY").gradeSchemeId(1L)
                .evaluationMethod("SCORE_RANGE")
                .gradeThresholds("[{\"gradeCode\":\"C\",\"value\":0}]")
                .build();
        InspTask t = task(600L, D1.plusDays(2));
        when(taskRepository.findByProjectIdAndTaskDateBetween(1L, D1, D2)).thenReturn(List.of(t));
        when(submissionRepository.findByTaskId(600L)).thenReturn(List.of(
                completedSub(41L, 600L, 10L, 888L, "Z", 90.0)
                // section 20 缺 → 用 max=90 填
        ));

        List<IndicatorResult> rs = engine.compute(ind, "2026-W21", D1, D2);
        assertThat(rs).hasSize(1);
        // (90 + 90) / 2 = 90
        assertThat(rs.get(0).getValue()).isEqualByComparingTo(new BigDecimal("90.0000"));
    }

    @Test
    @DisplayName("rankDirection=DESC: 高分 rank=1, 同分共享 rank")
    void rankDescTies() {
        Indicator ind = newIndicator(List.of(10L), MissingPolicy.IGNORE, RankDirection.DESC);
        InspTask t = task(700L, D1.plusDays(1));
        when(taskRepository.findByProjectIdAndTaskDateBetween(1L, D1, D2)).thenReturn(List.of(t));
        when(submissionRepository.findByTaskId(700L)).thenReturn(List.of(
                completedSub(51L, 700L, 10L, 1L, "T1", 95.0),
                completedSub(52L, 700L, 10L, 2L, "T2", 95.0),
                completedSub(53L, 700L, 10L, 3L, "T3", 70.0)
        ));

        List<IndicatorResult> rs = engine.compute(ind, "2026-W21", D1, D2);
        assertThat(rs).hasSize(3);
        Map<Long, Integer> rankByTarget = new LinkedHashMap<>();
        rs.forEach(r -> rankByTarget.put(r.getTargetId(), r.getRankPosition()));
        assertThat(rankByTarget.get(1L)).isEqualTo(1);
        assertThat(rankByTarget.get(2L)).isEqualTo(1);
        assertThat(rankByTarget.get(3L)).isEqualTo(3);
    }

    @Test
    @DisplayName("rankDirection=ASC: 低分 rank=1 (扣分越少越好)")
    void rankAsc() {
        Indicator ind = newIndicator(List.of(10L), MissingPolicy.IGNORE, RankDirection.ASC);
        InspTask t = task(800L, D1.plusDays(1));
        when(taskRepository.findByProjectIdAndTaskDateBetween(1L, D1, D2)).thenReturn(List.of(t));
        when(submissionRepository.findByTaskId(800L)).thenReturn(List.of(
                completedSub(61L, 800L, 10L, 1L, "T1", 10.0),
                completedSub(62L, 800L, 10L, 2L, "T2", 30.0)
        ));

        List<IndicatorResult> rs = engine.compute(ind, "2026-W21", D1, D2);
        Map<Long, Integer> rk = new LinkedHashMap<>();
        rs.forEach(r -> rk.put(r.getTargetId(), r.getRankPosition()));
        assertThat(rk.get(1L)).isEqualTo(1);
        assertThat(rk.get(2L)).isEqualTo(2);
    }

    @Test
    @DisplayName("no rank direction: rankPosition 留 null")
    void noRank() {
        Indicator ind = newIndicator(List.of(10L), MissingPolicy.IGNORE, null);
        InspTask t = task(900L, D1.plusDays(1));
        when(taskRepository.findByProjectIdAndTaskDateBetween(1L, D1, D2)).thenReturn(List.of(t));
        when(submissionRepository.findByTaskId(900L)).thenReturn(List.of(
                completedSub(71L, 900L, 10L, 1L, "T1", 88.0)
        ));

        List<IndicatorResult> rs = engine.compute(ind, "2026-W21", D1, D2);
        assertThat(rs).hasSize(1);
        assertThat(rs.get(0).getRankPosition()).isNull();
        assertThat(rs.get(0).getGrade()).isEqualTo("B");
    }
}
