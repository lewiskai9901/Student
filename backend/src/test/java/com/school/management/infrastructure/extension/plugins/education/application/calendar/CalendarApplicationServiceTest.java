package com.school.management.infrastructure.extension.plugins.education.application.calendar;

import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.extension.plugins.education.application.calendar.command.CreateAcademicYearCommand;
import com.school.management.infrastructure.extension.plugins.education.application.calendar.command.CreateSemesterCommand;
import com.school.management.infrastructure.extension.plugins.education.application.calendar.command.UpdateAcademicYearCommand;
import com.school.management.infrastructure.extension.plugins.education.domain.calendar.model.aggregate.AcademicYear;
import com.school.management.infrastructure.extension.plugins.education.domain.calendar.model.aggregate.Semester;
import com.school.management.infrastructure.extension.plugins.education.domain.calendar.repository.AcademicEventRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.calendar.repository.AcademicYearRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.calendar.repository.SemesterRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.calendar.repository.TeachingWeekRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Phase 6 W2 - CalendarApplicationService golden-path 测试.
 * 覆盖核心业务路径: 学年/学期 CRUD + 当前标记 + 冲突检测.
 * 不追求 100% 行覆盖, 只保证关键分支不退化.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarApplicationService 测试")
class CalendarApplicationServiceTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private SemesterRepository semesterRepository;
    @Mock private TeachingWeekRepository teachingWeekRepository;
    @Mock private AcademicEventRepository academicEventRepository;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private JdbcTemplate jdbc;

    @InjectMocks private CalendarApplicationService service;

    // ========== 工厂 ==========
    private AcademicYear year(Long id, String code, String name) {
        AcademicYear y = AcademicYear.create(code, name, LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31));
        setId(y, id);
        return y;
    }

    private Semester semester(Long id, Long yearId, String code, boolean current) {
        Semester s = Semester.create(yearId, "2026 秋", code,
            LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 15), 2026,
            com.school.management.infrastructure.extension.plugins.education.domain.calendar.model.valueobject.SemesterType.FIRST);
        setId(s, id);
        if (current) s.setAsCurrent();
        return s;
    }

    private void setId(Object aggregate, Long id) {
        // 反射绕过 AggregateRoot.id 的 protected setter
        try {
            java.lang.reflect.Field f = aggregate.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(aggregate, id);
        } catch (NoSuchFieldException e) {
            // fallback: 尝试父类
            try {
                java.lang.reflect.Field f = aggregate.getClass().getSuperclass().getDeclaredField("id");
                f.setAccessible(true);
                f.set(aggregate, id);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========== 学年 ==========

    @Nested
    @DisplayName("学年管理")
    class AcademicYearCases {

        @Test
        @DisplayName("createAcademicYear - 正常创建并落库")
        void create_ok() {
            CreateAcademicYearCommand cmd = new CreateAcademicYearCommand();
            cmd.setYearCode("2026-2027");
            cmd.setYearName("2026-2027 学年");
            cmd.setStartDate(LocalDate.of(2026, 9, 1));
            cmd.setEndDate(LocalDate.of(2027, 8, 31));

            when(academicYearRepository.save(any(AcademicYear.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            AcademicYear result = service.createAcademicYear(cmd);

            assertThat(result.getYearCode()).isEqualTo("2026-2027");
            assertThat(result.getYearName()).isEqualTo("2026-2027 学年");
            verify(academicYearRepository).save(any(AcademicYear.class));
        }

        @Test
        @DisplayName("updateAcademicYear - 不存在抛 BusinessException")
        void update_missing_throws() {
            when(academicYearRepository.findById(99L)).thenReturn(Optional.empty());
            UpdateAcademicYearCommand cmd = new UpdateAcademicYearCommand();
            cmd.setYearName("新名");
            cmd.setStartDate(LocalDate.now());
            cmd.setEndDate(LocalDate.now().plusYears(1));

            assertThatThrownBy(() -> service.updateAcademicYear(99L, cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("学年不存在");
        }

        @Test
        @DisplayName("setCurrentAcademicYear - 先 clearAll 再 save 目标")
        void set_current_clears_others() {
            AcademicYear y = year(1L, "2026", "2026-2027");
            when(academicYearRepository.findById(1L)).thenReturn(Optional.of(y));
            when(academicYearRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.setCurrentAcademicYear(1L);

            verify(academicYearRepository).clearAllCurrentFlags();
            verify(academicYearRepository, times(2)).findById(1L); // 验证实现里二次查
            assertThat(y.getIsCurrent()).isTrue();
        }
    }

    // ========== 学期 ==========

    @Nested
    @DisplayName("学期管理")
    class SemesterCases {

        @Test
        @DisplayName("createSemester - academicYearId 缺失抛 BusinessException")
        void create_without_year_rejected() {
            CreateSemesterCommand cmd = new CreateSemesterCommand();
            cmd.setSemesterName("春季");
            cmd.setStartDate(LocalDate.of(2027, 2, 1));
            cmd.setEndDate(LocalDate.of(2027, 6, 30));
            // academicYearId = null

            assertThatThrownBy(() -> service.createSemester(cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须选择所属学年");
        }

        @Test
        @DisplayName("createSemester - 学年不存在抛 BusinessException")
        void create_year_missing_rejected() {
            CreateSemesterCommand cmd = new CreateSemesterCommand();
            cmd.setAcademicYearId(999L);
            cmd.setSemesterName("春季");
            cmd.setStartDate(LocalDate.of(2027, 2, 1));
            cmd.setEndDate(LocalDate.of(2027, 6, 30));

            when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createSemester(cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("学年不存在");
        }

        @Test
        @DisplayName("createSemester - semesterCode 重复抛 BusinessException")
        void create_duplicate_code_rejected() {
            CreateSemesterCommand cmd = new CreateSemesterCommand();
            cmd.setAcademicYearId(1L);
            cmd.setSemesterName("秋季");
            cmd.setSemesterCode("2026-2027-1");
            cmd.setStartDate(LocalDate.of(2026, 9, 1));
            cmd.setEndDate(LocalDate.of(2027, 1, 15));
            cmd.setSemesterType(1);

            when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year(1L, "2026", "N")));
            when(semesterRepository.existsBySemesterCode("2026-2027-1")).thenReturn(true);

            assertThatThrownBy(() -> service.createSemester(cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("学期编码已存在");
        }

        @Test
        @DisplayName("createSemester - 正常创建")
        void create_ok() {
            CreateSemesterCommand cmd = new CreateSemesterCommand();
            cmd.setAcademicYearId(1L);
            cmd.setSemesterName("秋季");
            cmd.setStartDate(LocalDate.of(2026, 9, 1));
            cmd.setEndDate(LocalDate.of(2027, 1, 15));
            cmd.setSemesterType(1);

            when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year(1L, "2026", "N")));
            when(semesterRepository.existsBySemesterCode(any())).thenReturn(false);
            when(semesterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Semester result = service.createSemester(cmd);

            assertThat(result.getSemesterName()).isEqualTo("秋季");
            verify(semesterRepository).save(any(Semester.class));
        }

        @Test
        @DisplayName("deleteSemester - 当前学期不可删")
        void delete_current_rejected() {
            Semester s = semester(1L, 100L, "2026-2027-1", true);
            when(semesterRepository.findById(1L)).thenReturn(Optional.of(s));

            assertThatThrownBy(() -> service.deleteSemester(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除当前学期");

            verify(semesterRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("deleteSemester - 非当前学期可删")
        void delete_non_current_ok() {
            Semester s = semester(1L, 100L, "2026-2027-1", false);
            when(semesterRepository.findById(1L)).thenReturn(Optional.of(s));

            service.deleteSemester(1L);

            verify(semesterRepository).deleteById(1L);
        }

        @Test
        @DisplayName("setCurrentSemester - 清空旧标记 + 设置新当前")
        void set_current() {
            Semester s = semester(1L, 100L, "2026-2027-1", false);
            when(semesterRepository.findById(1L)).thenReturn(Optional.of(s));
            when(semesterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.setCurrentSemester(1L);

            verify(semesterRepository).clearAllCurrentFlags();
            assertThat(s.getIsCurrent()).isTrue();
        }

        @Test
        @DisplayName("endSemester - 非当前学期可结束")
        void end_semester() {
            Semester s = semester(1L, 100L, "2026-2027-1", false);
            when(semesterRepository.findById(1L)).thenReturn(Optional.of(s));
            when(semesterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.endSemester(1L);

            verify(semesterRepository).save(s);
        }
    }
}
