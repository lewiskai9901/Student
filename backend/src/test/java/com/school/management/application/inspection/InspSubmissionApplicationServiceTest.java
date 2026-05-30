package com.school.management.application.inspection;

import com.school.management.application.event.EntityEventApplicationService;
import com.school.management.application.event.TriggerService;
import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.repository.*;
import com.school.management.domain.inspection.service.ItemScoreEvaluator;
import com.school.management.domain.inspection.service.ObservationExtractor;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InspSubmissionApplicationService 应用服务单测.
 *
 * 用 Mockito 隔离 repository / 评分聚合 / 事件发布,
 * 验证 submission + detail + evidence 的 CRUD 与生命周期编排.
 *
 * 覆盖: createSubmission (含 sectionId 自动补) / lifecycle (lock/unlock/startFilling/
 *       saveFormData/completeSubmission/skipSubmission) / detail CRUD + 作答更新级联 /
 *       evidence CRUD / recalculateFromSubmission 委托.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspSubmissionApplicationService 应用服务")
class InspSubmissionApplicationServiceTest {

    @Mock InspSubmissionRepository submissionRepository;
    @Mock SubmissionDetailRepository detailRepository;
    @Mock InspEvidenceRepository evidenceRepository;
    @Mock InspTaskRepository taskRepository;
    @Mock InspProjectRepository projectRepository;
    @Mock ScoreAggregationService scoreAggregationService;
    @Mock SpringDomainEventPublisher eventPublisher;
    @Mock EntityEventApplicationService entityEventApplicationService;

    ObjectMapper objectMapper = new ObjectMapper();

    // 真实评分计算器 (无 FormulaEvaluator — 纯计算, FORMULA 模式退化为 0). 用于钉死
    // P1.6 服务端权威算分: updateDetailResponse 落库的 detail.score 由它算出, 忽略前端值.
    ItemScoreEvaluator itemScoreEvaluator = new ItemScoreEvaluator();

    InspSubmissionApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InspSubmissionApplicationService(
                submissionRepository, detailRepository, evidenceRepository,
                taskRepository, projectRepository, scoreAggregationService,
                eventPublisher, objectMapper, entityEventApplicationService,
                itemScoreEvaluator);
    }

    // ---- helpers ----

    private InspSubmission submissionInState(Long id, SubmissionStatus status) {
        return InspSubmission.reconstruct(InspSubmission.builder()
                .id(id).taskId(7L).sectionId(100L)
                .targetType(TargetType.ORG).targetId(33L).targetName("一班")
                .status(status));
    }

    private SubmissionDetail detail(Long id) {
        SubmissionDetail d = SubmissionDetail.create(50L, 5L, "I-1", "项目1", "SCORE");
        ReflectionTestUtils.setField(d, "id", id);
        return d;
    }

    // ============================================================
    @Nested
    @DisplayName("createSubmission")
    class CreateSubmissionTests {
        @Test
        @DisplayName("ORG 目标: orgUnitId=targetId, 保存 PENDING")
        void shouldCreateForOrgTarget() {
            // create() 已设 sectionId? 否 — InspSubmission.create 不设 sectionId
            when(taskRepository.findById(7L)).thenReturn(Optional.empty());
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission s = service.createSubmission(7L, TargetType.ORG, 33L, "一班");

            assertThat(s.getStatus()).isEqualTo(SubmissionStatus.PENDING);
            assertThat(s.getTargetId()).isEqualTo(33L);
            assertThat(s.getOrgUnitId()).isEqualTo(33L);
            verify(submissionRepository).save(any());
        }

        @Test
        @DisplayName("sectionId 为空时从项目 rootSectionId 自动补")
        void shouldAutofillSectionIdFromProject() {
            InspTask task = InspTask.reconstruct(InspTask.builder()
                    .id(7L).taskCode("TSK-1").projectId(9L)
                    .status(TaskStatus.PENDING));
            InspProject project = InspProject.builder().id(9L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).rootSectionId(500L).build();
            when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
            when(projectRepository.findById(9L)).thenReturn(Optional.of(project));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission s = service.createSubmission(7L, TargetType.ORG, 33L, "一班");

            assertThat(s.getSectionId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("项目无 rootSectionId: sectionId 保持 null")
        void shouldLeaveSectionNullWhenProjectHasNone() {
            InspTask task = InspTask.reconstruct(InspTask.builder()
                    .id(7L).taskCode("TSK-1").projectId(9L).status(TaskStatus.PENDING));
            InspProject project = InspProject.builder().id(9L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).build();
            when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
            when(projectRepository.findById(9L)).thenReturn(Optional.of(project));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission s = service.createSubmission(7L, TargetType.ORG, 33L, "一班");
            assertThat(s.getSectionId()).isNull();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("查询")
    class QueryTests {
        @Test
        @DisplayName("getSubmission 委托 repository")
        void shouldGet() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.PENDING);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            assertThat(service.getSubmission(1L)).containsSame(s);
        }

        @Test
        @DisplayName("listSubmissionsByTask 委托 repository")
        void shouldListByTask() {
            when(submissionRepository.findByTaskId(7L)).thenReturn(List.of());
            assertThat(service.listSubmissionsByTask(7L)).isEmpty();
        }

        @Test
        @DisplayName("listSubmissionsByTarget 委托 repository")
        void shouldListByTarget() {
            when(submissionRepository.findByTargetId(33L)).thenReturn(List.of());
            assertThat(service.listSubmissionsByTarget(33L)).isEmpty();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("生命周期 — lock / unlock / startFilling / saveFormData / skip")
    class LifecycleTests {
        @Test
        @DisplayName("lockSubmission: PENDING → LOCKED")
        void shouldLock() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.PENDING);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission saved = service.lockSubmission(1L);
            assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.LOCKED);
        }

        @Test
        @DisplayName("lockSubmission: 不存在抛 IllegalArgumentException")
        void shouldRejectLockNotFound() {
            when(submissionRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.lockSubmission(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("提交不存在");
        }

        @Test
        @DisplayName("unlockSubmission: LOCKED → PENDING")
        void shouldUnlock() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.LOCKED);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission saved = service.unlockSubmission(1L);
            assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        }

        @Test
        @DisplayName("startFilling: PENDING → IN_PROGRESS")
        void shouldStartFilling() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.PENDING);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission saved = service.startFilling(1L);
            assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("saveFormData: IN_PROGRESS 保存表单 + syncVersion+1")
        void shouldSaveFormData() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.IN_PROGRESS);
            int beforeSync = s.getSyncVersion();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission saved = service.saveFormData(1L, "{\"a\":1}");
            assertThat(saved.getFormData()).isEqualTo("{\"a\":1}");
            assertThat(saved.getSyncVersion()).isEqualTo(beforeSync + 1);
        }

        @Test
        @DisplayName("skipSubmission: PENDING → SKIPPED")
        void shouldSkip() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.PENDING);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission saved = service.skipSubmission(1L);
            assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.SKIPPED);
        }

        @Test
        @DisplayName("skipSubmission: IN_PROGRESS 状态下抛 IllegalStateException")
        void shouldRejectSkipFromInProgress() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.IN_PROGRESS);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            assertThatThrownBy(() -> service.skipSubmission(1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("completeSubmission — 完成 + 后端算分")
    class CompleteSubmissionTests {

        private ScoreAggregationService.ScoreFields fields() {
            return new ScoreAggregationService.ScoreFields(
                    new BigDecimal("100"), new BigDecimal("92"),
                    new BigDecimal("8"), BigDecimal.ZERO,
                    "{}", "A", Boolean.TRUE);
        }

        @Test
        @DisplayName("IN_PROGRESS 提交: 计算分数 → COMPLETED + 发事件 + 更新 task 计数")
        void shouldCompleteSuccessfully() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.IN_PROGRESS);
            InspTask task = InspTask.reconstruct(InspTask.builder()
                    .id(7L).taskCode("TSK-1").projectId(9L).status(TaskStatus.IN_PROGRESS));
            InspProject project = InspProject.builder().id(9L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).build();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
            when(projectRepository.findById(9L)).thenReturn(Optional.of(project));
            when(detailRepository.findBySubmissionId(1L)).thenReturn(List.of());
            // 评分配置下沉: computeScoreFields 首参改为解析后的 Long profileId
            when(scoreAggregationService.computeScoreFields(any(), anyList(), eq(100L),
                    eq("ORG_UNIT"), eq(33L))).thenReturn(fields());
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(submissionRepository.findByTaskId(7L)).thenReturn(List.of(s));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission saved = service.completeSubmission(1L);

            assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
            assertThat(saved.getFinalScore()).isEqualByComparingTo("92");
            assertThat(saved.getGrade()).isEqualTo("A");
            assertThat(saved.getPassed()).isTrue();
            verify(eventPublisher).publishAll(any());
            // updateTaskCompletedCount: task 计数被更新
            ArgumentCaptor<InspTask> taskCap = ArgumentCaptor.forClass(InspTask.class);
            verify(taskRepository).save(taskCap.capture());
            assertThat(taskCap.getValue().getCompletedTargets()).isEqualTo(1);
        }

        @Test
        @DisplayName("PENDING 提交: 自动转 IN_PROGRESS 后完成")
        void shouldAutoTransitionFromPending() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.PENDING);
            InspTask task = InspTask.reconstruct(InspTask.builder()
                    .id(7L).taskCode("TSK-1").projectId(9L).status(TaskStatus.IN_PROGRESS));
            InspProject project = InspProject.builder().id(9L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).build();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
            when(projectRepository.findById(9L)).thenReturn(Optional.of(project));
            when(detailRepository.findBySubmissionId(1L)).thenReturn(List.of());
            when(scoreAggregationService.computeScoreFields(any(), anyList(), any(), any(), any()))
                    .thenReturn(fields());
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(submissionRepository.findByTaskId(7L)).thenReturn(List.of(s));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission saved = service.completeSubmission(1L);
            assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        }

        @Test
        @DisplayName("submission 不存在抛 IllegalArgumentException")
        void shouldRejectWhenSubmissionMissing() {
            when(submissionRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.completeSubmission(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("提交不存在");
        }

        @Test
        @DisplayName("关联 task 不存在抛 IllegalStateException")
        void shouldRejectWhenTaskMissing() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.IN_PROGRESS);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(taskRepository.findById(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.completeSubmission(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("任务不存在");
        }

        @Test
        @DisplayName("关联 project 不存在抛 IllegalStateException")
        void shouldRejectWhenProjectMissing() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.IN_PROGRESS);
            InspTask task = InspTask.reconstruct(InspTask.builder()
                    .id(7L).taskCode("TSK-1").projectId(9L).status(TaskStatus.IN_PROGRESS));
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
            when(projectRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.completeSubmission(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("项目不存在");
        }

        @Test
        @DisplayName("computeScoreFields 抛异常: 向上传播")
        void shouldPropagateScoreError() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.IN_PROGRESS);
            InspTask task = InspTask.reconstruct(InspTask.builder()
                    .id(7L).taskCode("TSK-1").projectId(9L).status(TaskStatus.IN_PROGRESS));
            InspProject project = InspProject.builder().id(9L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).build();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
            when(projectRepository.findById(9L)).thenReturn(Optional.of(project));
            when(detailRepository.findBySubmissionId(1L)).thenReturn(List.of());
            when(scoreAggregationService.computeScoreFields(any(), anyList(), any(), any(), any()))
                    .thenThrow(new RuntimeException("算分失败"));

            assertThatThrownBy(() -> service.completeSubmission(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("算分失败");
        }

        @Test
        @DisplayName("旧签名 completeSubmission(8 参) 委托到新方法")
        void shouldDelegateLegacyOverload() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.IN_PROGRESS);
            InspTask task = InspTask.reconstruct(InspTask.builder()
                    .id(7L).taskCode("TSK-1").projectId(9L).status(TaskStatus.IN_PROGRESS));
            InspProject project = InspProject.builder().id(9L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).build();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
            when(projectRepository.findById(9L)).thenReturn(Optional.of(project));
            when(detailRepository.findBySubmissionId(1L)).thenReturn(List.of());
            when(scoreAggregationService.computeScoreFields(any(), anyList(), any(), any(), any()))
                    .thenReturn(fields());
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(submissionRepository.findByTaskId(7L)).thenReturn(List.of(s));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission saved = service.completeSubmission(1L, BigDecimal.TEN,
                    BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, "{}", "B", false);
            assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("recalculateFromSubmission — 级联重算委托")
    class RecalculateTests {
        @Test
        @DisplayName("委托 ScoreAggregationService.recalculateFromSubmission")
        void shouldDelegate() {
            InspSubmission s = submissionInState(1L, SubmissionStatus.COMPLETED);
            when(scoreAggregationService.recalculateFromSubmission(1L)).thenReturn(s);
            assertThat(service.recalculateFromSubmission(1L)).isSameAs(s);
            verify(scoreAggregationService).recalculateFromSubmission(1L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("Submission Details — CRUD")
    class DetailCrudTests {
        @Test
        @DisplayName("createDetail (5 参) 保存明细")
        void shouldCreateDetailBasic() {
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            SubmissionDetail d = service.createDetail(50L, 5L, "I-1", "项目1", "SCORE");
            assertThat(d.getSubmissionId()).isEqualTo(50L);
            assertThat(d.getItemCode()).isEqualTo("I-1");
        }

        @Test
        @DisplayName("createDetail (8 参) 带 section + scoringMode")
        void shouldCreateDetailWithSection() {
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            SubmissionDetail d = service.createDetail(50L, 5L, "I-1", "项目1", "SCORE",
                    100L, "分区A", ScoringMode.DEDUCTION);
            assertThat(d.getSectionId()).isEqualTo(100L);
            assertThat(d.getScoringMode()).isEqualTo(ScoringMode.DEDUCTION);
        }

        @Test
        @DisplayName("createDetail (11 参) 带 scoringConfig")
        void shouldCreateDetailFull() {
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            SubmissionDetail d = service.createDetail(50L, 5L, "I-1", "项目1", "SCORE",
                    100L, "分区A", ScoringMode.DEDUCTION, "{}", "[]", null);
            assertThat(d.getScoringConfig()).isEqualTo("{}");
        }

        @Test
        @DisplayName("getDetail / listDetailsBySubmission / listFlaggedDetails 委托 repository")
        void shouldQueryDetails() {
            SubmissionDetail d = detail(1L);
            when(detailRepository.findById(1L)).thenReturn(Optional.of(d));
            when(detailRepository.findBySubmissionId(50L)).thenReturn(List.of(d));
            when(detailRepository.findFlaggedBySubmissionId(50L)).thenReturn(List.of());

            assertThat(service.getDetail(1L)).containsSame(d);
            assertThat(service.listDetailsBySubmission(50L)).hasSize(1);
            assertThat(service.listFlaggedDetails(50L)).isEmpty();
        }

        @Test
        @DisplayName("updateDetailResponse: submission 未完成时不触发级联重算")
        void shouldUpdateResponseWithoutCascade() {
            SubmissionDetail d = detail(1L);
            when(detailRepository.findById(1L)).thenReturn(Optional.of(d));
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspSubmission s = submissionInState(50L, SubmissionStatus.IN_PROGRESS);
            when(submissionRepository.findById(50L)).thenReturn(Optional.of(s));

            SubmissionDetail result = service.updateDetailResponse(1L, "yes",
                    ScoringMode.DEDUCTION, new BigDecimal("5"), null);

            assertThat(result).isNotNull();
            verify(scoreAggregationService, never()).recalculateFromSubmission(anyLong());
        }

        @Test
        @DisplayName("updateDetailResponse: submission 已完成时触发级联重算")
        void shouldUpdateResponseWithCascade() {
            SubmissionDetail d = detail(1L);
            when(detailRepository.findById(1L)).thenReturn(Optional.of(d));
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspSubmission s = submissionInState(50L, SubmissionStatus.COMPLETED);
            when(submissionRepository.findById(50L)).thenReturn(Optional.of(s));

            service.updateDetailResponse(1L, "no", ScoringMode.DEDUCTION,
                    new BigDecimal("3"), null);

            verify(scoreAggregationService).recalculateFromSubmission(50L);
        }

        @Test
        @DisplayName("P1.6: updateDetailResponse 用 ItemScoreEvaluator 算权威 detail.score, 忽略前端 score")
        void shouldComputeAuthoritativeScoreIgnoringFrontend() {
            // LEVEL 模式, scoringConfig 含 levels[{良:8}], responseValue=良 → 权威分 8
            SubmissionDetail d = SubmissionDetail.create(50L, 5L, "I-1", "等级项", "SCORE",
                    100L, "分区A", ScoringMode.LEVEL,
                    "{\"levels\":[{\"label\":\"优\",\"score\":\"10\"},{\"label\":\"良\",\"score\":\"8\"}]}",
                    null, null);
            ReflectionTestUtils.setField(d, "id", 1L);
            when(detailRepository.findById(1L)).thenReturn(Optional.of(d));
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspSubmission s = submissionInState(50L, SubmissionStatus.IN_PROGRESS);
            when(submissionRepository.findById(50L)).thenReturn(Optional.of(s));

            // 前端乱传 score=999 — 必须被忽略
            service.updateDetailResponse(1L, "良", ScoringMode.LEVEL,
                    new BigDecimal("999"), null);

            ArgumentCaptor<SubmissionDetail> cap = ArgumentCaptor.forClass(SubmissionDetail.class);
            verify(detailRepository).save(cap.capture());
            // 落库的 detail.score = 服务端权威算出的 8, 不是前端的 999
            assertThat(cap.getValue().getScore()).isEqualByComparingTo("8");
        }

        @Test
        @DisplayName("updateDetailResponse: 明细不存在抛 IllegalArgumentException")
        void shouldRejectWhenDetailMissing() {
            when(detailRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateDetailResponse(1L, "x",
                    ScoringMode.DEDUCTION, BigDecimal.ONE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("明细不存在");
        }

        @Test
        @DisplayName("updateDetailRemark 更新备注")
        void shouldUpdateRemark() {
            SubmissionDetail d = detail(1L);
            when(detailRepository.findById(1L)).thenReturn(Optional.of(d));
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubmissionDetail result = service.updateDetailRemark(1L, "注意此项");
            assertThat(result.getRemark()).isEqualTo("注意此项");
        }

        @Test
        @DisplayName("flagDetail 标记异常项")
        void shouldFlagDetail() {
            SubmissionDetail d = detail(1L);
            when(detailRepository.findById(1L)).thenReturn(Optional.of(d));
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubmissionDetail result = service.flagDetail(1L, "数据可疑");
            assertThat(result.getIsFlagged()).isTrue();
            assertThat(result.getFlagReason()).isEqualTo("数据可疑");
        }

        @Test
        @DisplayName("unflagDetail 取消标记")
        void shouldUnflagDetail() {
            SubmissionDetail d = detail(1L);
            d.flag("曾标记");
            when(detailRepository.findById(1L)).thenReturn(Optional.of(d));
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubmissionDetail result = service.unflagDetail(1L);
            assertThat(result.getIsFlagged()).isFalse();
        }

        @Test
        @DisplayName("flagDetail: 明细不存在抛")
        void shouldRejectFlagWhenMissing() {
            when(detailRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.flagDetail(1L, "r"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deleteDetail 委托 repository.deleteById")
        void shouldDeleteDetail() {
            service.deleteDetail(99L);
            verify(detailRepository).deleteById(99L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("Evidence — CRUD")
    class EvidenceTests {
        @Test
        @DisplayName("addEvidence 创建并保存证据")
        void shouldAddEvidence() {
            when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspEvidence e = service.addEvidence(50L, 5L, EvidenceType.PHOTO,
                    "photo.jpg", "http://x/photo.jpg");
            assertThat(e.getSubmissionId()).isEqualTo(50L);
            assertThat(e.getEvidenceType()).isEqualTo(EvidenceType.PHOTO);
            assertThat(e.getFileName()).isEqualTo("photo.jpg");
        }

        @Test
        @DisplayName("listEvidenceBySubmission / listEvidenceByDetail 委托 repository")
        void shouldListEvidence() {
            when(evidenceRepository.findBySubmissionId(50L)).thenReturn(List.of());
            when(evidenceRepository.findByDetailId(5L)).thenReturn(List.of());
            assertThat(service.listEvidenceBySubmission(50L)).isEmpty();
            assertThat(service.listEvidenceByDetail(5L)).isEmpty();
        }

        @Test
        @DisplayName("deleteEvidence 委托 repository.deleteById")
        void shouldDeleteEvidence() {
            service.deleteEvidence(77L);
            verify(evidenceRepository).deleteById(77L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("publishInspectionEvents — 完成时触发事件 (有 extractor 注入)")
    class PublishEventsTests {
        @Test
        @DisplayName("triggerService 已注入但 observationExtractors 为空: 跳过事件发布仍正常完成")
        void shouldCompleteWhenExtractorsAbsent() {
            ReflectionTestUtils.setField(service, "triggerService", mock(TriggerService.class));
            // observationExtractors 保持 null
            InspSubmission s = submissionInState(1L, SubmissionStatus.IN_PROGRESS);
            InspTask task = InspTask.reconstruct(InspTask.builder()
                    .id(7L).taskCode("TSK-1").projectId(9L).status(TaskStatus.IN_PROGRESS));
            InspProject project = InspProject.builder().id(9L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).build();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(s));
            when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
            when(projectRepository.findById(9L)).thenReturn(Optional.of(project));
            when(detailRepository.findBySubmissionId(1L)).thenReturn(List.of());
            when(scoreAggregationService.computeScoreFields(any(), anyList(), any(), any(), any()))
                    .thenReturn(new ScoreAggregationService.ScoreFields(
                            BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                            "{}", "A", true));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(submissionRepository.findByTaskId(7L)).thenReturn(List.of(s));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspSubmission saved = service.completeSubmission(1L);
            assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        }
    }
}
