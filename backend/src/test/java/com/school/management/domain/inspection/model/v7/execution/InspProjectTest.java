package com.school.management.domain.inspection.model.v7.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InspProject 聚合根测试")
class InspProjectTest {

    // ==================== 工厂方法 ====================

    private InspProject createDraftProject() {
        return InspProject.create("PROJ-001", "卫生检查项目",
                100L, LocalDate.of(2026, 3, 1), 1L);
    }

    private InspProject createPublishedProject() {
        InspProject project = createDraftProject();
        project.publish(200L);
        return project;
    }

    private InspProject createPausedProject() {
        InspProject project = createPublishedProject();
        project.pause();
        return project;
    }

    private InspProject createCompletedProject() {
        InspProject project = createPublishedProject();
        project.complete();
        return project;
    }

    // ==================== 创建测试 ====================

    @Nested
    @DisplayName("创建项目")
    class CreateTests {

        @Test
        @DisplayName("新建项目初始状态为DRAFT")
        void testCreate() {
            // When
            InspProject project = InspProject.create("PROJ-001", "卫生检查项目",
                    100L, LocalDate.of(2026, 3, 1), 1L);

            // Then
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.DRAFT);
            assertThat(project.getProjectCode()).isEqualTo("PROJ-001");
            assertThat(project.getProjectName()).isEqualTo("卫生检查项目");
            assertThat(project.getRootSectionId()).isEqualTo(100L);
            assertThat(project.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(project.getCreatedBy()).isEqualTo(1L);
            assertThat(project.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("新建项目默认值正确")
        void testCreate_Defaults() {
            InspProject project = createDraftProject();

            assertThat(project.getScopeType()).isEqualTo(ScopeType.ORG);
            assertThat(project.getAssignmentMode()).isEqualTo(AssignmentMode.ASSIGNED);
            assertThat(project.getReviewRequired()).isTrue();
            assertThat(project.getAutoPublish()).isFalse();
        }
    }

    // ==================== 发布测试 ====================

    @Nested
    @DisplayName("发布项目")
    class PublishTests {

        @Test
        @DisplayName("DRAFT -> PUBLISHED 正常发布")
        void testPublish() {
            // Given
            InspProject project = createDraftProject();

            // When
            project.publish(200L);

            // Then
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.PUBLISHED);
            assertThat(project.getTemplateVersionId()).isEqualTo(200L);
            assertThat(project.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("非DRAFT状态发布应抛异常")
        void testPublish_InvalidState() {
            InspProject project = createPublishedProject();

            assertThatThrownBy(() -> project.publish(300L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有草稿项目才能发布");
        }
    }

    // ==================== 暂停测试 ====================

    @Nested
    @DisplayName("暂停项目")
    class PauseTests {

        @Test
        @DisplayName("PUBLISHED -> PAUSED 正常暂停")
        void testPause() {
            // Given
            InspProject project = createPublishedProject();

            // When
            project.pause();

            // Then
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.PAUSED);
        }

        @Test
        @DisplayName("非PUBLISHED状态暂停应抛异常")
        void testPause_InvalidState() {
            InspProject project = createDraftProject();

            assertThatThrownBy(() -> project.pause())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已发布的项目才能暂停");
        }
    }

    // ==================== 恢复测试 ====================

    @Nested
    @DisplayName("恢复项目")
    class ResumeTests {

        @Test
        @DisplayName("PAUSED -> PUBLISHED 正常恢复")
        void testResume() {
            // Given
            InspProject project = createPausedProject();

            // When
            project.resume();

            // Then
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.PUBLISHED);
        }

        @Test
        @DisplayName("非PAUSED状态恢复应抛异常")
        void testResume_InvalidState() {
            InspProject project = createPublishedProject();

            assertThatThrownBy(() -> project.resume())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已暂停的项目才能恢复");
        }
    }

    // ==================== 完成测试 ====================

    @Nested
    @DisplayName("完成项目")
    class CompleteTests {

        @Test
        @DisplayName("PUBLISHED -> COMPLETED 正常完成")
        void testComplete_FromPublished() {
            InspProject project = createPublishedProject();

            project.complete();

            assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        }

        @Test
        @DisplayName("PAUSED -> COMPLETED 从暂停状态完成")
        void testComplete_FromPaused() {
            InspProject project = createPausedProject();

            project.complete();

            assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        }

        @Test
        @DisplayName("DRAFT 状态完成应抛异常")
        void testComplete_InvalidState() {
            InspProject project = createDraftProject();

            assertThatThrownBy(() -> project.complete())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已发布或已暂停的项目才能完成");
        }
    }

    // ==================== 归档测试 ====================

    @Nested
    @DisplayName("归档项目")
    class ArchiveTests {

        @Test
        @DisplayName("COMPLETED -> ARCHIVED 正常归档")
        void testArchive() {
            InspProject project = createCompletedProject();

            project.archive();

            assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        }

        @Test
        @DisplayName("非COMPLETED状态归档应抛异常")
        void testArchive_InvalidState() {
            InspProject project = createPublishedProject();

            assertThatThrownBy(() -> project.archive())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已完成的项目才能归档");
        }
    }

    // ==================== 更新信息测试 ====================

    @Nested
    @DisplayName("更新项目信息")
    class UpdateInfoTests {

        @Test
        @DisplayName("DRAFT 状态可以更新信息")
        void testUpdateInfo_DraftAllowed() {
            InspProject project = createDraftProject();

            project.updateInfo("新项目名称", 200L, 10L,
                    ScopeType.ORG, null,
                    LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30),
                    AssignmentMode.ASSIGNED, false, true, 2L);

            assertThat(project.getProjectName()).isEqualTo("新项目名称");
            assertThat(project.getRootSectionId()).isEqualTo(200L);
            assertThat(project.getAutoPublish()).isTrue();
        }

        @Test
        @DisplayName("非DRAFT状态更新信息应抛异常")
        void testUpdateInfo_NonDraftThrows() {
            InspProject project = createPublishedProject();

            assertThatThrownBy(() -> project.updateInfo("新名称", 200L, 10L,
                    ScopeType.ORG, null,
                    LocalDate.now(), null,
                    AssignmentMode.ASSIGNED, true, false, 2L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有草稿状态的项目才能修改");
        }
    }

    // ==================== 非法状态转换测试 ====================

    @Nested
    @DisplayName("非法状态转换")
    class InvalidTransitionTests {

        @Test
        @DisplayName("DRAFT -> PAUSED 应抛异常")
        void testDraftToPausedThrows() {
            InspProject project = createDraftProject();

            assertThatThrownBy(() -> project.pause())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("DRAFT -> COMPLETED 应抛异常")
        void testDraftToCompletedThrows() {
            InspProject project = createDraftProject();

            assertThatThrownBy(() -> project.complete())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("DRAFT -> ARCHIVED 应抛异常")
        void testDraftToArchivedThrows() {
            InspProject project = createDraftProject();

            assertThatThrownBy(() -> project.archive())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("ARCHIVED -> PUBLISHED 应抛异常（终态）")
        void testArchivedToPublishedThrows() {
            InspProject project = createCompletedProject();
            project.archive();

            assertThatThrownBy(() -> project.publish(300L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
