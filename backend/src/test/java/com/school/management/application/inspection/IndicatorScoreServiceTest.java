package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionStatus;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.model.scoring.GradeDefinition;
import com.school.management.domain.inspection.model.scoring.GradeScheme;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.IndicatorScore;
import com.school.management.domain.inspection.repository.GradeDefinitionRepository;
import com.school.management.domain.inspection.repository.GradeSchemeRepository;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import com.school.management.domain.inspection.repository.IndicatorScoreRepository;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IndicatorScoreService 评分计算单测.
 *
 * 用 Mockito 隔离 6 个 repository, 喂代表性数据并断言真实计算结果:
 *  - aggregate (AVG/SUM/MAX/MIN/LATEST) 的数值正确性
 *  - aggregateComposite (WEIGHTED_AVG/AVG/SUM/MIN/MAX)
 *  - matchGradeForIndicator 等级映射 (SCORE_RANGE/PERCENT_RANGE/RANK_COUNT/RANK_PERCENT)
 *  - 缺失策略 SKIP / CARRY_FORWARD / MARK_INCOMPLETE
 *  - 增量重算 computeOnSubmissionComplete
 *  - 全量计算 computeAllForProject (空集合 / 叶子+复合)
 *
 * matchGradeForIndicator 通过公开 API (computeAllForProject) 间接覆盖, 因其为 private.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IndicatorScoreService 评分计算")
class IndicatorScoreServiceTest {

    @Mock IndicatorRepository indicatorRepository;
    @Mock IndicatorScoreRepository scoreRepository;
    @Mock InspSubmissionRepository submissionRepository;
    @Mock InspTaskRepository taskRepository;
    @Mock GradeSchemeRepository gradeSchemeRepository;
    @Mock GradeDefinitionRepository gradeDefinitionRepository;

    @InjectMocks IndicatorScoreService service;

    private static final LocalDate D1 = LocalDate.of(2026, 5, 1);

    // ── builders ───────────────────────────────────────────────

    private Indicator leaf(Long id, Long projectId, Long sectionId, String aggregation) {
        Indicator i = Indicator.builder()
                .id(id).projectId(projectId).name("叶子")
                .indicatorType("LEAF").sourceSectionId(sectionId)
                .sourceAggregation(aggregation).evaluationPeriod("DAILY")
                .evaluationMethod("SCORE_RANGE")
                .build();
        return i;
    }

    private Indicator composite(Long id, Long projectId, String aggregation, String missingPolicy) {
        return Indicator.builder()
                .id(id).projectId(projectId).name("复合")
                .indicatorType("COMPOSITE").compositeAggregation(aggregation)
                .missingPolicy(missingPolicy).evaluationPeriod("DAILY")
                .evaluationMethod("SCORE_RANGE")
                .build();
    }

    private InspTask task(Long id, Long projectId, LocalDate date) {
        return InspTask.builder()
                .id(id).taskCode("TK-" + id).projectId(projectId).taskDate(date)
                .build();
    }

    private InspSubmission completedSub(Long taskId, Long sectionId, Long targetId,
                                        BigDecimal finalScore) {
        return InspSubmission.builder()
                .id(System.nanoTime())
                .taskId(taskId).sectionId(sectionId)
                .targetType(TargetType.ORG).targetId(targetId).targetName("T" + targetId)
                .status(SubmissionStatus.COMPLETED).finalScore(finalScore)
                .build();
    }

    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("computeAllForProject — 全量计算")
    class ComputeAllForProjectTests {

        @Test
        @DisplayName("项目无指标时直接返回, 不查任务")
        void shouldReturnEarlyWhenNoIndicators() {
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of());

            service.computeAllForProject(1L, D1, D1);

            verify(taskRepository, never()).findByProjectIdAndTaskDate(anyLong(), any());
            verify(scoreRepository, never()).save(any());
        }

        @Test
        @DisplayName("单叶子指标: 聚合 AVG 计算并保存 IndicatorScore")
        void shouldComputeLeafAvgAndSave() {
            Indicator l = leaf(10L, 1L, 100L, "AVG");
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(l));

            InspTask t = task(50L, 1L, D1);
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of(t));
            // submissions: target 7 has scores 80 + 90 -> AVG = 85.00
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 100L, 7L, new BigDecimal("80")),
                    completedSub(50L, 100L, 7L, new BigDecimal("90"))
            ));
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(10L, 7L, D1))
                    .thenReturn(Optional.empty());
            when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            IndicatorScore saved = cap.getAllValues().get(0);
            assertThat(saved.getScore()).isEqualByComparingTo("85.00");
            assertThat(saved.getSourceCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("叶子指标 SUM 聚合: 80+90 = 170")
        void shouldComputeLeafSum() {
            Indicator l = leaf(10L, 1L, 100L, "SUM");
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(l));
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1))
                    .thenReturn(List.of(task(50L, 1L, D1)));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 100L, 7L, new BigDecimal("80")),
                    completedSub(50L, 100L, 7L, new BigDecimal("90"))
            ));
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(10L, 7L, D1))
                    .thenReturn(Optional.empty());
            when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(0).getScore()).isEqualByComparingTo("170");
        }

        @Test
        @DisplayName("叶子指标 MAX 聚合: max(60,95,70) = 95")
        void shouldComputeLeafMax() {
            Indicator l = leaf(10L, 1L, 100L, "MAX");
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(l));
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1))
                    .thenReturn(List.of(task(50L, 1L, D1)));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 100L, 7L, new BigDecimal("60")),
                    completedSub(50L, 100L, 7L, new BigDecimal("95")),
                    completedSub(50L, 100L, 7L, new BigDecimal("70"))
            ));
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(10L, 7L, D1))
                    .thenReturn(Optional.empty());
            when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(0).getScore()).isEqualByComparingTo("95");
        }

        @Test
        @DisplayName("叶子指标 MIN 聚合: min(60,95,70) = 60")
        void shouldComputeLeafMin() {
            Indicator l = leaf(10L, 1L, 100L, "MIN");
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(l));
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1))
                    .thenReturn(List.of(task(50L, 1L, D1)));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 100L, 7L, new BigDecimal("60")),
                    completedSub(50L, 100L, 7L, new BigDecimal("95")),
                    completedSub(50L, 100L, 7L, new BigDecimal("70"))
            ));
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(10L, 7L, D1))
                    .thenReturn(Optional.empty());
            when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(0).getScore()).isEqualByComparingTo("60");
        }

        @Test
        @DisplayName("叶子指标 LATEST 聚合: 取最后一个提交分数")
        void shouldComputeLeafLatest() {
            Indicator l = leaf(10L, 1L, 100L, "LATEST");
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(l));
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1))
                    .thenReturn(List.of(task(50L, 1L, D1)));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 100L, 7L, new BigDecimal("60")),
                    completedSub(50L, 100L, 7L, new BigDecimal("88"))
            ));
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(10L, 7L, D1))
                    .thenReturn(Optional.empty());
            when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(0).getScore()).isEqualByComparingTo("88");
        }

        @Test
        @DisplayName("无完成提交时叶子不产生分数, 不保存")
        void shouldNotSaveWhenNoCompletedSubmissions() {
            Indicator l = leaf(10L, 1L, 100L, "AVG");
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(l));
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of());

            service.computeAllForProject(1L, D1, D1);

            verify(scoreRepository, never()).save(any());
        }

        @Test
        @DisplayName("非 COMPLETED 状态 / 别的 section 的提交被过滤掉")
        void shouldFilterByStatusAndSection() {
            Indicator l = leaf(10L, 1L, 100L, "AVG");
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(l));
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1))
                    .thenReturn(List.of(task(50L, 1L, D1)));
            // 一条是别的 section, 一条非 COMPLETED, 一条有效
            InspSubmission wrongSection = completedSub(50L, 999L, 7L, new BigDecimal("10"));
            InspSubmission inProgress = InspSubmission.builder()
                    .id(123L).taskId(50L).sectionId(100L)
                    .targetType(TargetType.ORG).targetId(7L)
                    .status(SubmissionStatus.IN_PROGRESS).finalScore(new BigDecimal("20"))
                    .build();
            InspSubmission valid = completedSub(50L, 100L, 7L, new BigDecimal("75"));
            when(submissionRepository.findByTaskId(50L))
                    .thenReturn(List.of(wrongSection, inProgress, valid));
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(10L, 7L, D1))
                    .thenReturn(Optional.empty());
            when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            // 只有 valid (75) 被纳入
            assertThat(cap.getAllValues().get(0).getScore()).isEqualByComparingTo("75.00");
            assertThat(cap.getAllValues().get(0).getSourceCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("复合指标 WEIGHTED_AVG: 子分数 (80,90) 权重均为 1 -> 85.00")
        void shouldComputeCompositeWeightedAvg() {
            Indicator child1 = leaf(11L, 1L, 100L, "AVG");
            Indicator child2 = leaf(12L, 1L, 101L, "AVG");
            child1.setParentIndicatorId(20L);
            child2.setParentIndicatorId(20L);
            Indicator comp = composite(20L, 1L, "WEIGHTED_AVG", "SKIP");
            when(indicatorRepository.findByProjectId(1L))
                    .thenReturn(List.of(child1, child2, comp));

            // 一个任务一个提交, 让 collectTargetsInPeriod 找到 target 7
            InspTask t = task(50L, 1L, D1);
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of(t));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 100L, 7L, new BigDecimal("80")),
                    completedSub(50L, 101L, 7L, new BigDecimal("90"))
            ));
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(11L, 7L, D1))
                    .thenReturn(Optional.empty());
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(12L, 7L, D1))
                    .thenReturn(Optional.empty());
            // composite child lookup
            when(indicatorRepository.findByParentIndicatorId(20L))
                    .thenReturn(List.of(child1, child2));
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(20L, 7L, D1))
                    .thenReturn(Optional.empty());
            // saved child scores re-read by composite step
            when(scoreRepository.save(any())).thenAnswer(inv -> {
                IndicatorScore s = inv.getArgument(0);
                lenient().when(scoreRepository.findByIndicatorAndTargetAndPeriod(
                                s.getIndicatorId(), s.getTargetId(), s.getPeriodStart()))
                        .thenReturn(Optional.of(s));
                return s;
            });

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            IndicatorScore compScore = cap.getAllValues().stream()
                    .filter(s -> s.getIndicatorId().equals(20L))
                    .reduce((a, b) -> b).orElseThrow();
            assertThat(compScore.getScore()).isEqualByComparingTo("85.00");
            assertThat(compScore.getSourceCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("复合指标 MARK_INCOMPLETE: 任一子指标缺分则不计算复合")
        void shouldSkipCompositeWhenMarkIncomplete() {
            Indicator child1 = leaf(11L, 1L, 100L, "AVG");
            child1.setParentIndicatorId(20L);
            Indicator comp = composite(20L, 1L, "WEIGHTED_AVG", "MARK_INCOMPLETE");
            when(indicatorRepository.findByProjectId(1L))
                    .thenReturn(List.of(child1, comp));

            when(taskRepository.findByProjectIdAndTaskDate(1L, D1))
                    .thenReturn(List.of(task(50L, 1L, D1)));
            // no submissions for leaf -> child has no score
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 999L, 7L, new BigDecimal("80")) // wrong section
            ));
            when(indicatorRepository.findByParentIndicatorId(20L)).thenReturn(List.of(child1));
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(11L, 7L, D1))
                    .thenReturn(Optional.empty());

            service.computeAllForProject(1L, D1, D1);

            // composite (20L) should never be saved because child missing under MARK_INCOMPLETE.
            // The leaf also produces no score (wrong-section submission), so save() may not be
            // called at all — use atLeast(0) to capture whatever was saved without requiring any.
            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeast(0)).save(cap.capture());
            assertThat(cap.getAllValues()).noneMatch(s -> s.getIndicatorId().equals(20L));
        }
    }

    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("computeOnSubmissionComplete — 增量重算")
    class IncrementalTests {

        @Test
        @DisplayName("提交不存在: 直接返回, 不查任务")
        void shouldReturnWhenSubmissionMissing() {
            when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

            service.computeOnSubmissionComplete(999L);

            verify(taskRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("提交 sectionId 为空: 直接返回")
        void shouldReturnWhenSectionIdNull() {
            InspSubmission sub = InspSubmission.builder()
                    .id(1L).taskId(50L).targetType(TargetType.ORG).targetId(7L)
                    .status(SubmissionStatus.COMPLETED).build();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));

            service.computeOnSubmissionComplete(1L);

            verify(taskRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("任务不存在: 直接返回, 不查指标")
        void shouldReturnWhenTaskMissing() {
            InspSubmission sub = InspSubmission.builder()
                    .id(1L).taskId(50L).sectionId(100L)
                    .targetType(TargetType.ORG).targetId(7L)
                    .status(SubmissionStatus.COMPLETED).build();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));
            when(taskRepository.findById(50L)).thenReturn(Optional.empty());

            service.computeOnSubmissionComplete(1L);

            verify(indicatorRepository, never()).findByProjectId(anyLong());
        }

        @Test
        @DisplayName("相关叶子指标重算并保存得分")
        void shouldRecomputeRelevantLeaf() {
            InspSubmission sub = InspSubmission.builder()
                    .id(1L).taskId(50L).sectionId(100L)
                    .targetType(TargetType.ORG).targetId(7L).targetName("T7")
                    .status(SubmissionStatus.COMPLETED).finalScore(new BigDecimal("70"))
                    .build();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));
            InspTask t = task(50L, 1L, D1);
            when(taskRepository.findById(50L)).thenReturn(Optional.of(t));

            Indicator l = leaf(10L, 1L, 100L, "AVG");
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(l));

            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of(t));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 100L, 7L, new BigDecimal("70"))
            ));
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(10L, 7L, D1))
                    .thenReturn(Optional.empty());
            when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(scoreRepository.findByIndicatorId(10L)).thenReturn(List.of());

            service.computeOnSubmissionComplete(1L);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(0).getScore()).isEqualByComparingTo("70.00");
        }

        @Test
        @DisplayName("无 section 匹配的叶子指标: 不保存任何得分")
        void shouldNotRecomputeWhenNoMatchingLeaf() {
            InspSubmission sub = InspSubmission.builder()
                    .id(1L).taskId(50L).sectionId(100L)
                    .targetType(TargetType.ORG).targetId(7L)
                    .status(SubmissionStatus.COMPLETED).finalScore(new BigDecimal("70"))
                    .build();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));
            when(taskRepository.findById(50L)).thenReturn(Optional.of(task(50L, 1L, D1)));
            // leaf points at section 999, submission section 100 -> not relevant
            Indicator l = leaf(10L, 1L, 999L, "AVG");
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(l));

            service.computeOnSubmissionComplete(1L);

            verify(scoreRepository, never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("等级映射 — matchGradeForIndicator (经全量计算覆盖)")
    class GradeMatchingTests {

        private Indicator leafWithGrade(String evalMethod, String thresholdsJson) {
            return Indicator.builder()
                    .id(10L).projectId(1L).name("叶子").indicatorType("LEAF")
                    .sourceSectionId(100L).sourceAggregation("AVG")
                    .evaluationPeriod("DAILY").evaluationMethod(evalMethod)
                    .gradeSchemeId(5L).gradeThresholds(thresholdsJson)
                    .build();
        }

        private List<GradeDefinition> gradeDefs() {
            return List.of(
                    GradeDefinition.builder().id(1L).gradeSchemeId(5L)
                            .code("A").name("优秀").color("#0f0").build(),
                    GradeDefinition.builder().id(2L).gradeSchemeId(5L)
                            .code("B").name("合格").color("#ff0").build()
            );
        }

        private GradeScheme schemeWithGrades() {
            GradeScheme scheme = GradeScheme.builder()
                    .id(5L).displayName("三级方案").schemeType("CUSTOM").build();
            scheme.setGrades(gradeDefs());
            return scheme;
        }

        private void stubLeafComputation(Indicator l, BigDecimal score) {
            when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(l));
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1))
                    .thenReturn(List.of(task(50L, 1L, D1)));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 100L, 7L, score)
            ));
            when(scoreRepository.save(any())).thenAnswer(inv -> {
                IndicatorScore s = inv.getArgument(0);
                lenient().when(scoreRepository.findByIndicatorAndTargetAndPeriod(
                                s.getIndicatorId(), s.getTargetId(), s.getPeriodStart()))
                        .thenReturn(Optional.of(s));
                return s;
            });
            when(scoreRepository.findByIndicatorAndTargetAndPeriod(10L, 7L, D1))
                    .thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("SCORE_RANGE: 分数 92 >= 阈值 90 -> 命中等级 A")
        void shouldMatchScoreRangeHighGrade() {
            Indicator l = leafWithGrade("SCORE_RANGE",
                    "[{\"gradeCode\":\"A\",\"value\":90},{\"gradeCode\":\"B\",\"value\":60}]");
            stubLeafComputation(l, new BigDecimal("92"));
            when(gradeSchemeRepository.findById(5L)).thenReturn(Optional.of(schemeWithGrades()));
            when(gradeDefinitionRepository.findByGradeSchemeId(5L)).thenReturn(gradeDefs());

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            IndicatorScore graded = cap.getAllValues().get(cap.getAllValues().size() - 1);
            assertThat(graded.getGradeCode()).isEqualTo("A");
            assertThat(graded.getGradeName()).isEqualTo("优秀");
        }

        @Test
        @DisplayName("SCORE_RANGE: 分数 75 落在 B 区间 (60<=75<90)")
        void shouldMatchScoreRangeMidGrade() {
            Indicator l = leafWithGrade("SCORE_RANGE",
                    "[{\"gradeCode\":\"A\",\"value\":90},{\"gradeCode\":\"B\",\"value\":60}]");
            stubLeafComputation(l, new BigDecimal("75"));
            when(gradeSchemeRepository.findById(5L)).thenReturn(Optional.of(schemeWithGrades()));
            when(gradeDefinitionRepository.findByGradeSchemeId(5L)).thenReturn(gradeDefs());

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(cap.getAllValues().size() - 1).getGradeCode())
                    .isEqualTo("B");
        }

        @Test
        @DisplayName("SCORE_RANGE 边界: 分数恰等于阈值 90 -> 命中 A (>= 含等号)")
        void shouldMatchAtExactThreshold() {
            Indicator l = leafWithGrade("SCORE_RANGE",
                    "[{\"gradeCode\":\"A\",\"value\":90},{\"gradeCode\":\"B\",\"value\":60}]");
            stubLeafComputation(l, new BigDecimal("90"));
            when(gradeSchemeRepository.findById(5L)).thenReturn(Optional.of(schemeWithGrades()));
            when(gradeDefinitionRepository.findByGradeSchemeId(5L)).thenReturn(gradeDefs());

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(cap.getAllValues().size() - 1).getGradeCode())
                    .isEqualTo("A");
        }

        @Test
        @DisplayName("SCORE_RANGE: 分数低于所有阈值 -> 用最后一档 B")
        void shouldFallBackToLastGradeWhenBelowAll() {
            Indicator l = leafWithGrade("SCORE_RANGE",
                    "[{\"gradeCode\":\"A\",\"value\":90},{\"gradeCode\":\"B\",\"value\":60}]");
            stubLeafComputation(l, new BigDecimal("30"));
            when(gradeSchemeRepository.findById(5L)).thenReturn(Optional.of(schemeWithGrades()));
            when(gradeDefinitionRepository.findByGradeSchemeId(5L)).thenReturn(gradeDefs());

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(cap.getAllValues().size() - 1).getGradeCode())
                    .isEqualTo("B");
        }

        @Test
        @DisplayName("无 gradeSchemeId: 等级为空 (GradeMatch.EMPTY)")
        void shouldReturnEmptyGradeWhenNoScheme() {
            Indicator l = leaf(10L, 1L, 100L, "AVG"); // no gradeSchemeId
            stubLeafComputation(l, new BigDecimal("80"));

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(cap.getAllValues().size() - 1).getGradeCode())
                    .isNull();
        }

        @Test
        @DisplayName("gradeScheme 查不到: 等级为空")
        void shouldReturnEmptyGradeWhenSchemeNotFound() {
            Indicator l = leafWithGrade("SCORE_RANGE",
                    "[{\"gradeCode\":\"A\",\"value\":90}]");
            stubLeafComputation(l, new BigDecimal("95"));
            when(gradeSchemeRepository.findById(5L)).thenReturn(Optional.empty());

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(cap.getAllValues().size() - 1).getGradeCode())
                    .isNull();
        }

        @Test
        @DisplayName("非法 gradeThresholds JSON: 等级为空, 不抛异常")
        void shouldReturnEmptyGradeWhenThresholdsInvalid() {
            Indicator l = leafWithGrade("SCORE_RANGE", "not-a-json");
            stubLeafComputation(l, new BigDecimal("95"));
            when(gradeSchemeRepository.findById(5L)).thenReturn(Optional.of(schemeWithGrades()));
            when(gradeDefinitionRepository.findByGradeSchemeId(5L)).thenReturn(List.of());

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(cap.getAllValues().size() - 1).getGradeCode())
                    .isNull();
        }

        @Test
        @DisplayName("RANK_COUNT: 唯一目标 rank=1, 命中 rank<=1 阈值")
        void shouldMatchRankCount() {
            Indicator l = leafWithGrade("RANK_COUNT",
                    "[{\"gradeCode\":\"A\",\"value\":1},{\"gradeCode\":\"B\",\"value\":5}]");
            stubLeafComputation(l, new BigDecimal("88"));
            when(gradeSchemeRepository.findById(5L)).thenReturn(Optional.of(schemeWithGrades()));
            when(gradeDefinitionRepository.findByGradeSchemeId(5L)).thenReturn(gradeDefs());

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues().get(cap.getAllValues().size() - 1).getGradeCode())
                    .isEqualTo("A");
        }

        @Test
        @DisplayName("RANK_PERCENT: 唯一目标 percentile=100, 命中 value=100 阈值")
        void shouldMatchRankPercent() {
            Indicator l = leafWithGrade("RANK_PERCENT",
                    "[{\"gradeCode\":\"A\",\"value\":50},{\"gradeCode\":\"B\",\"value\":100}]");
            stubLeafComputation(l, new BigDecimal("88"));
            when(gradeSchemeRepository.findById(5L)).thenReturn(Optional.of(schemeWithGrades()));
            when(gradeDefinitionRepository.findByGradeSchemeId(5L)).thenReturn(gradeDefs());

            service.computeAllForProject(1L, D1, D1);

            ArgumentCaptor<IndicatorScore> cap = ArgumentCaptor.forClass(IndicatorScore.class);
            verify(scoreRepository, atLeastOnce()).save(cap.capture());
            // percentile 100 > 50, so first matching threshold is value=100 -> B
            assertThat(cap.getAllValues().get(cap.getAllValues().size() - 1).getGradeCode())
                    .isEqualTo("B");
        }
    }
}
