package com.school.management.domain.inspection.model.execution;

import com.school.management.domain.inspection.event.ProjectCompletedEvent;
import com.school.management.domain.inspection.event.ProjectPausedEvent;
import com.school.management.domain.inspection.event.ProjectPublishedEvent;
import com.school.management.domain.inspection.event.ProjectResumedEvent;
import com.school.management.domain.shared.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * InspProject 聚合根状态机单测.
 * Track 1: 业务护栏, 锁定 P0/P1 整改成果, 防回退.
 *
 * 覆盖:
 *   - 5 个状态 (DRAFT/PUBLISHED/PAUSED/COMPLETED/ARCHIVED) 全部合法迁移
 *   - 所有非法迁移 (各状态对错误动作的 IllegalStateException)
 *   - autoPublish + reviewRequired 互斥
 *   - 模板版本升级 (relockTemplateVersion)
 *   - 项目级策略 (updatePolicyConfig) 边界
 *   - 领域事件正确注册
 */
@DisplayName("InspProject 聚合根状态机")
class InspProjectStateMachineTest {

    private static InspProject newDraft() {
        return InspProject.create("PRJ-T-001", "测试项目", 100L, LocalDate.of(2026, 5, 1), 50L, 999L);
    }

    private static InspProject inState(ProjectStatus status) {
        return InspProject.builder()
                .id(1L).projectCode("PRJ-T-001").projectName("测试项目")
                .rootSectionId(100L).status(status).createdBy(999L)
                .build();
    }

    // ============================================================
    @Nested
    @DisplayName("create — 工厂方法")
    class CreateTests {
        @Test
        @DisplayName("创建后状态为 DRAFT, 字段默认值正确")
        void shouldCreateInDraftWithDefaults() {
            InspProject p = newDraft();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.DRAFT);
            assertThat(p.getProjectCode()).isEqualTo("PRJ-T-001");
            assertThat(p.getCreatedBy()).isEqualTo(999L);
            assertThat(p.getCreatedAt()).isNotNull();
            // 默认值
            assertThat(p.getReviewRequired()).isTrue();
            assertThat(p.getAutoPublish()).isFalse();
            assertThat(p.getScopeType()).isEqualTo(ScopeType.ORG);
            assertThat(p.getAssignmentMode()).isEqualTo(AssignmentMode.ASSIGNED);
            assertThat(p.getEvaluationMode()).isEqualTo("SINGLE");
            assertThat(p.getDomainEvents()).isEmpty();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("publish — 发布")
    class PublishTests {
        @Test
        @DisplayName("DRAFT → PUBLISHED 成功且注册 ProjectPublishedEvent")
        void shouldPublishFromDraft() {
            InspProject p = newDraft();
            p.publish(500L);
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.PUBLISHED);
            assertThat(p.getTemplateVersionId()).isEqualTo(500L);
            List<DomainEvent> events = p.getDomainEvents();
            assertThat(events).hasSize(1).first().isInstanceOf(ProjectPublishedEvent.class);
        }

        @Test
        @DisplayName("PUBLISHED → publish 抛 IllegalStateException (重复发布)")
        void shouldRejectDoublePublish() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            assertThatThrownBy(() -> p.publish(500L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("草稿");
        }

        @Test
        @DisplayName("PAUSED → publish 抛 (只能 resume)")
        void shouldRejectPublishFromPaused() {
            InspProject p = inState(ProjectStatus.PAUSED);
            assertThatThrownBy(() -> p.publish(500L)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("autoPublish=true + reviewRequired=true 时 publish 抛 (互斥)")
        void shouldRejectPublishWhenAutoPublishAndReviewBothTrue() {
            InspProject p = InspProject.builder()
                    .projectCode("X").projectName("X").rootSectionId(1L)
                    .status(ProjectStatus.DRAFT)
                    .autoPublish(true).reviewRequired(true).build();
            assertThatThrownBy(() -> p.publish(500L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("自动发布");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("pause — 暂停")
    class PauseTests {
        @Test
        @DisplayName("PUBLISHED → PAUSED 成功且注册 ProjectPausedEvent")
        void shouldPauseFromPublished() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            p.pause();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.PAUSED);
            assertThat(p.getDomainEvents()).hasSize(1).first().isInstanceOf(ProjectPausedEvent.class);
        }

        @Test
        @DisplayName("DRAFT → pause 抛")
        void shouldRejectPauseFromDraft() {
            InspProject p = newDraft();
            assertThatThrownBy(p::pause).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("COMPLETED → pause 抛")
        void shouldRejectPauseFromCompleted() {
            InspProject p = inState(ProjectStatus.COMPLETED);
            assertThatThrownBy(p::pause).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("PAUSED → pause 抛 (重复暂停)")
        void shouldRejectDoublePause() {
            InspProject p = inState(ProjectStatus.PAUSED);
            assertThatThrownBy(p::pause).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("resume — 恢复")
    class ResumeTests {
        @Test
        @DisplayName("PAUSED → PUBLISHED 成功且注册 ProjectResumedEvent")
        void shouldResumeFromPaused() {
            InspProject p = inState(ProjectStatus.PAUSED);
            p.resume();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.PUBLISHED);
            assertThat(p.getDomainEvents()).hasSize(1).first().isInstanceOf(ProjectResumedEvent.class);
        }

        @Test
        @DisplayName("PUBLISHED → resume 抛")
        void shouldRejectResumeFromPublished() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            assertThatThrownBy(p::resume).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("DRAFT → resume 抛")
        void shouldRejectResumeFromDraft() {
            InspProject p = newDraft();
            assertThatThrownBy(p::resume).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("complete — 完结 (不可逆)")
    class CompleteTests {
        @Test
        @DisplayName("PUBLISHED → COMPLETED 成功且注册 ProjectCompletedEvent")
        void shouldCompleteFromPublished() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            p.complete();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
            assertThat(p.getDomainEvents()).hasSize(1).first().isInstanceOf(ProjectCompletedEvent.class);
        }

        @Test
        @DisplayName("PAUSED → COMPLETED 成功 (允许从暂停直接完结)")
        void shouldCompleteFromPaused() {
            InspProject p = inState(ProjectStatus.PAUSED);
            p.complete();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        }

        @Test
        @DisplayName("DRAFT → complete 抛")
        void shouldRejectCompleteFromDraft() {
            assertThatThrownBy(newDraft()::complete).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("COMPLETED → complete 抛 (重复完结)")
        void shouldRejectDoubleComplete() {
            InspProject p = inState(ProjectStatus.COMPLETED);
            assertThatThrownBy(p::complete).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("archive — 归档")
    class ArchiveTests {
        @Test
        @DisplayName("COMPLETED → ARCHIVED 成功")
        void shouldArchiveFromCompleted() {
            InspProject p = inState(ProjectStatus.COMPLETED);
            p.archive();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        }

        @Test
        @DisplayName("PUBLISHED → archive 抛 (必须先 complete)")
        void shouldRejectArchiveFromPublished() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            assertThatThrownBy(p::archive).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("ARCHIVED → archive 抛 (重复)")
        void shouldRejectDoubleArchive() {
            InspProject p = inState(ProjectStatus.ARCHIVED);
            assertThatThrownBy(p::archive).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("updateInfo — 完整修改 (仅 DRAFT)")
    class UpdateInfoTests {
        @Test
        @DisplayName("DRAFT → updateInfo 成功")
        void shouldUpdateDraft() {
            InspProject p = newDraft();
            p.updateInfo("新名", 200L, 300L, ScopeType.ORG, "[1,2]",
                    LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1),
                    AssignmentMode.FREE, true, false, 999L);
            assertThat(p.getProjectName()).isEqualTo("新名");
            assertThat(p.getScopeConfig()).isEqualTo("[1,2]");
            assertThat(p.getAssignmentMode()).isEqualTo(AssignmentMode.FREE);
        }

        @Test
        @DisplayName("PUBLISHED → updateInfo 抛 (已发布不可全量修改)")
        void shouldRejectUpdateInfoOnPublished() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            assertThatThrownBy(() -> p.updateInfo("X", 1L, null, null, null,
                    null, null, null, true, false, 1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("updateInfo 中 autoPublish + reviewRequired 同时启用时抛")
        void shouldRejectAutoPublishConflict() {
            InspProject p = newDraft();
            assertThatThrownBy(() -> p.updateInfo("X", 1L, null, ScopeType.ORG, null,
                    null, null, AssignmentMode.FREE, true, true, 999L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("updateOperationalConfig — 运行时配置")
    class UpdateOperationalConfigTests {
        @Test
        @DisplayName("PUBLISHED 状态下修改 assignmentMode 成功")
        void shouldUpdateAssignmentModeOnPublished() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            p.updateOperationalConfig(AssignmentMode.FREE, null, null, null, 999L);
            assertThat(p.getAssignmentMode()).isEqualTo(AssignmentMode.FREE);
        }

        @Test
        @DisplayName("ARCHIVED → updateOperationalConfig 抛")
        void shouldRejectUpdateConfigOnArchived() {
            InspProject p = inState(ProjectStatus.ARCHIVED);
            assertThatThrownBy(() -> p.updateOperationalConfig(AssignmentMode.FREE, null, null, null, 1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("合并后 autoPublish + reviewRequired 冲突时抛")
        void shouldDetectConflictAfterMerge() {
            // 起初 reviewRequired=true, 现在试图开启 autoPublish=true → 冲突
            InspProject p = InspProject.builder()
                    .projectCode("X").projectName("X").rootSectionId(1L)
                    .status(ProjectStatus.PUBLISHED)
                    .reviewRequired(true).autoPublish(false).build();
            assertThatThrownBy(() -> p.updateOperationalConfig(null, null, true, null, 1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("relockTemplateVersion — 模板快照升级")
    class RelockTemplateVersionTests {
        @Test
        @DisplayName("PUBLISHED → relockTemplateVersion 成功")
        void shouldRelockFromPublished() {
            InspProject p = InspProject.builder().projectCode("X").projectName("X").rootSectionId(1L)
                    .status(ProjectStatus.PUBLISHED).templateVersionId(10L).build();
            p.relockTemplateVersion(20L);
            assertThat(p.getTemplateVersionId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("PAUSED → relockTemplateVersion 成功")
        void shouldRelockFromPaused() {
            InspProject p = InspProject.builder().projectCode("X").projectName("X").rootSectionId(1L)
                    .status(ProjectStatus.PAUSED).templateVersionId(10L).build();
            p.relockTemplateVersion(20L);
            assertThat(p.getTemplateVersionId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("DRAFT → relockTemplateVersion 抛 (草稿不需要)")
        void shouldRejectRelockOnDraft() {
            InspProject p = newDraft();
            assertThatThrownBy(() -> p.relockTemplateVersion(20L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("relockTemplateVersion(null) 抛")
        void shouldRejectNullVersion() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            assertThatThrownBy(() -> p.relockTemplateVersion(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("updatePolicyConfig — 项目级策略 (review/E)")
    class UpdatePolicyConfigTests {
        @Test
        @DisplayName("PUBLISHED → 修改三个策略字段成功")
        void shouldUpdatePolicyOnPublished() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            p.updatePolicyConfig(5, 4, 14, 999L);
            assertThat(p.getMaxRejectCount()).isEqualTo(5);
            assertThat(p.getMaxEscalationLevel()).isEqualTo(4);
            assertThat(p.getAppealWindowDays()).isEqualTo(14);
        }

        @Test
        @DisplayName("ARCHIVED → updatePolicyConfig 抛")
        void shouldRejectPolicyOnArchived() {
            InspProject p = inState(ProjectStatus.ARCHIVED);
            assertThatThrownBy(() -> p.updatePolicyConfig(5, 4, 14, 999L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("maxRejectCount = -1 抛")
        void shouldRejectNegativeMaxRejectCount() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            assertThatThrownBy(() -> p.updatePolicyConfig(-1, null, null, 999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("appealWindowDays = -1 抛")
        void shouldRejectNegativeAppealWindow() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            assertThatThrownBy(() -> p.updatePolicyConfig(null, null, -1, 999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("全 null 等同于沿用系统默认 (无异常)")
        void shouldAcceptAllNull() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            p.updatePolicyConfig(null, null, null, 999L);
            assertThat(p.getMaxRejectCount()).isNull();
            assertThat(p.getAppealWindowDays()).isNull();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("lockScoringConfig — 评分配置快照")
    class LockScoringConfigTests {
        @Test
        @DisplayName("快照可在任意状态写入")
        void shouldLockSnapshot() {
            InspProject p = inState(ProjectStatus.PUBLISHED);
            p.lockScoringConfig("{\"a\":1}");
            assertThat(p.getScoringConfigSnapshot()).isEqualTo("{\"a\":1}");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("完整生命周期路径")
    class FullLifecycleTests {
        @Test
        @DisplayName("DRAFT → PUBLISHED → PAUSED → PUBLISHED → COMPLETED → ARCHIVED")
        void shouldWalkFullLifecycle() {
            InspProject p = newDraft();
            p.publish(500L);
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.PUBLISHED);
            p.pause();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.PAUSED);
            p.resume();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.PUBLISHED);
            p.complete();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
            p.archive();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
            // 4 个事件: published, paused, resumed, completed (archive 不发事件)
            assertThat(p.getDomainEvents()).hasSize(4);
        }

        @Test
        @DisplayName("DRAFT → PUBLISHED → COMPLETED → ARCHIVED (跳过 PAUSED 的最短路径)")
        void shouldWalkShortestLifecycle() {
            InspProject p = newDraft();
            p.publish(500L);
            p.complete();
            p.archive();
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        }
    }
}
