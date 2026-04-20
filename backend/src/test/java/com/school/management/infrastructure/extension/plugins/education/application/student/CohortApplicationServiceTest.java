package com.school.management.infrastructure.extension.plugins.education.application.student;

import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.infrastructure.extension.plugins.education.application.student.command.AssignCohortLeaderCommand;
import com.school.management.infrastructure.extension.plugins.education.application.student.command.CreateCohortCommand;
import com.school.management.infrastructure.extension.plugins.education.application.student.command.UpdateCohortCommand;
import com.school.management.infrastructure.extension.plugins.education.application.student.query.CohortDTO;
import com.school.management.infrastructure.extension.plugins.education.domain.student.model.Cohort;
import com.school.management.infrastructure.extension.plugins.education.domain.student.model.CohortStatus;
import com.school.management.infrastructure.extension.plugins.education.domain.student.repository.CohortRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.student.repository.SchoolClassRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Phase 6 W2 - CohortApplicationService golden-path 测试.
 * 覆盖: 创建重复校验, 更新, 状态转换 (active/graduate/stopEnrollment), 删除校验.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CohortApplicationService 测试")
class CohortApplicationServiceTest {

    @Mock private CohortRepository cohortRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private DomainEventPublisher eventPublisher;

    @InjectMocks private CohortApplicationService service;

    private Cohort cohort(Long id) {
        Cohort c = Cohort.create("2026", "2026 级", 2026, 3, 1L);
        setId(c, id);
        return c;
    }

    private void setId(Object aggregate, Long id) {
        // Cohort 等聚合用私有 id 遮蔽了 AggregateRoot 的 id, 必须按类本身查
        Class<?> cls = aggregate.getClass();
        while (cls != null) {
            try {
                java.lang.reflect.Field f = cls.getDeclaredField("id");
                f.setAccessible(true);
                f.set(aggregate, id);
                return;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("No id field found in " + aggregate.getClass());
    }

    @Nested
    @DisplayName("创建")
    class Create {
        @Test
        @DisplayName("createCohort - 正常创建")
        void ok() {
            CreateCohortCommand cmd = new CreateCohortCommand();
            cmd.setGradeCode("2026");
            cmd.setGradeName("2026 级");
            cmd.setEnrollmentYear(2026);
            cmd.setSchoolingYears(3);
            cmd.setCreatedBy(1L);

            when(cohortRepository.existsByGradeCode("2026")).thenReturn(false);
            when(cohortRepository.existsByEnrollmentYear(2026)).thenReturn(false);
            when(cohortRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CohortDTO result = service.createCohort(cmd);

            assertThat(result.getGradeCode()).isEqualTo("2026");
            verify(cohortRepository).save(any(Cohort.class));
        }

        @Test
        @DisplayName("createCohort - gradeCode 重复拒绝")
        void duplicate_grade_code() {
            CreateCohortCommand cmd = new CreateCohortCommand();
            cmd.setGradeCode("2026");
            cmd.setEnrollmentYear(2026);

            when(cohortRepository.existsByGradeCode("2026")).thenReturn(true);

            assertThatThrownBy(() -> service.createCohort(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("年级编码已存在");

            verify(cohortRepository, never()).save(any());
        }

        @Test
        @DisplayName("createCohort - enrollmentYear 重复拒绝")
        void duplicate_enrollment_year() {
            CreateCohortCommand cmd = new CreateCohortCommand();
            cmd.setGradeCode("2026X");
            cmd.setEnrollmentYear(2026);

            when(cohortRepository.existsByGradeCode("2026X")).thenReturn(false);
            when(cohortRepository.existsByEnrollmentYear(2026)).thenReturn(true);

            assertThatThrownBy(() -> service.createCohort(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("入学年份已存在年级");
        }
    }

    @Nested
    @DisplayName("更新 / 委派")
    class UpdateAndAssign {
        @Test
        @DisplayName("updateCohort - 不存在抛 IllegalArgumentException")
        void not_found() {
            when(cohortRepository.findById(99L)).thenReturn(Optional.empty());
            UpdateCohortCommand cmd = new UpdateCohortCommand();
            cmd.setGradeName("X");

            assertThatThrownBy(() -> service.updateCohort(99L, cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("年级不存在");
        }

        @Test
        @DisplayName("assignLeaders - 仅 directorId 时只 assignDirector")
        void assign_director_only() {
            Cohort c = cohort(1L);
            when(cohortRepository.findById(1L)).thenReturn(Optional.of(c));
            when(cohortRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AssignCohortLeaderCommand cmd = new AssignCohortLeaderCommand();
            cmd.setDirectorId(10L);
            cmd.setDirectorName("王校长");
            cmd.setUpdatedBy(1L);

            service.assignLeaders(1L, cmd);

            assertThat(c.getDirectorId()).isEqualTo(10L);
            assertThat(c.getDirectorName()).isEqualTo("王校长");
            assertThat(c.getCounselorId()).isNull();
        }
    }

    @Nested
    @DisplayName("状态转换")
    class StatusTransitions {
        @Test
        @DisplayName("activateCohort - 调用 aggregate.activate + save")
        void activate() {
            Cohort c = cohort(1L);
            when(cohortRepository.findById(1L)).thenReturn(Optional.of(c));
            when(cohortRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.activateCohort(1L, 1L);

            assertThat(c.getStatus()).isEqualTo(CohortStatus.ACTIVE);
            verify(cohortRepository).save(c);
        }

        @Test
        @DisplayName("graduateCohort - 需先 activate")
        void graduate_requires_active() {
            Cohort c = cohort(1L);
            c.activate(1L); // 先切到 ACTIVE 才能 graduate
            when(cohortRepository.findById(1L)).thenReturn(Optional.of(c));
            when(cohortRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.graduateCohort(1L, 1L);

            assertThat(c.getStatus()).isEqualTo(CohortStatus.GRADUATED);
        }

        @Test
        @DisplayName("stopEnrollment - 需先 activate")
        void stop_enrollment_requires_active() {
            Cohort c = cohort(1L);
            c.activate(1L);
            when(cohortRepository.findById(1L)).thenReturn(Optional.of(c));
            when(cohortRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.stopEnrollment(1L, 1L);

            assertThat(c.getStatus()).isEqualTo(CohortStatus.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("查询")
    class Query {
        @Test
        @DisplayName("getCohort - 不存在抛异常")
        void get_not_found() {
            when(cohortRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getCohort(99L))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getCohort - 返回 DTO 字段映射")
        void get_ok_dto_mapped() {
            Cohort c = cohort(1L);
            when(cohortRepository.findById(1L)).thenReturn(Optional.of(c));

            CohortDTO dto = service.getCohort(1L);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getGradeCode()).isEqualTo("2026");
            assertThat(dto.getEnrollmentYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("getAllCohorts - 列表映射")
        void get_all() {
            when(cohortRepository.findAllOrderByEnrollmentYearDesc())
                .thenReturn(List.of(cohort(1L), cohort(2L)));

            List<CohortDTO> dtos = service.getAllCohorts();

            assertThat(dtos).hasSize(2);
        }

        @Test
        @DisplayName("getActiveCohorts - 代理到 findActiveCohorts")
        void get_active() {
            when(cohortRepository.findActiveCohorts()).thenReturn(Collections.emptyList());

            assertThat(service.getActiveCohorts()).isEmpty();
            verify(cohortRepository).findActiveCohorts();
        }
    }

    @Nested
    @DisplayName("删除")
    class Delete {
        @Test
        @DisplayName("deleteCohort - 有班级时拒绝")
        void delete_with_classes_rejected() {
            Cohort c = cohort(1L);
            when(cohortRepository.findById(1L)).thenReturn(Optional.of(c));
            when(schoolClassRepository.countByGradeId(1L)).thenReturn(3);

            assertThatThrownBy(() -> service.deleteCohort(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("还有 3 个班级");

            verify(cohortRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("deleteCohort - 无班级时放行")
        void delete_empty_ok() {
            Cohort c = cohort(1L);
            when(cohortRepository.findById(1L)).thenReturn(Optional.of(c));
            when(schoolClassRepository.countByGradeId(1L)).thenReturn(0);

            service.deleteCohort(1L);

            verify(cohortRepository).deleteById(1L);
        }
    }
}
