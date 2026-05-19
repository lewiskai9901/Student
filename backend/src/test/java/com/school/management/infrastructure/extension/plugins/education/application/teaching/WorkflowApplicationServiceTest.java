package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * WorkflowApplicationService 单测 — 验证流水线 currentStep 推断和 teachingWeeks 取值
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowApplicationService 测试")
class WorkflowApplicationServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private WorkflowApplicationService service;

    private static final Long SEMESTER_ID = 1L;

    private void stubCounts(long offeringTotal, long offeringConfirmed,
                            long assignTotal, long assignConfirmed,
                            long taskTotal, long teacherAssigned, long scheduled) {
        // SQL 路由 — 单参 varargs Object 形式
        lenient().when(jdbc.queryForObject(anyString(), eq(Long.class), eq(SEMESTER_ID)))
                .thenAnswer(inv -> {
                    String sql = inv.getArgument(0);
                    if (sql.contains("semester_course_offerings") && sql.contains("status = 1")) return offeringConfirmed;
                    if (sql.contains("semester_course_offerings")) return offeringTotal;
                    if (sql.contains("class_course_assignments") && sql.contains("status = 1")) return assignConfirmed;
                    if (sql.contains("class_course_assignments")) return assignTotal;
                    if (sql.contains("JOIN teaching_task_teachers")) return teacherAssigned;
                    if (sql.contains("scheduling_status = 2")) return scheduled;
                    if (sql.contains("teaching_tasks")) return taskTotal;
                    return null;
                });
        // academic_weeks 查询返回 null,落到默认 teachingWeeks=16
        lenient().when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(SEMESTER_ID)))
                .thenReturn(null);
    }

    @Test
    @DisplayName("offerings 全为 0 时 currentStep=offering_create")
    void shouldReturnOfferingCreateWhenZero() {
        stubCounts(0, 0, 0, 0, 0, 0, 0);

        Map<String, Object> result = service.getStats(SEMESTER_ID);

        assertThat(result.get("currentStep")).isEqualTo("offering_create");
        assertThat(result.get("teachingWeeks")).isEqualTo(16);
    }

    @Test
    @DisplayName("offerings 未确认完应 currentStep=offering_confirm")
    void shouldReturnOfferingConfirm() {
        stubCounts(10, 5, 0, 0, 0, 0, 0);
        assertThat(service.getStats(SEMESTER_ID).get("currentStep")).isEqualTo("offering_confirm");
    }

    @Test
    @DisplayName("class assignments 为 0 时 currentStep=class_assign")
    void shouldReturnClassAssign() {
        stubCounts(10, 10, 0, 0, 0, 0, 0);
        assertThat(service.getStats(SEMESTER_ID).get("currentStep")).isEqualTo("class_assign");
    }

    @Test
    @DisplayName("tasks=0 时 currentStep=generate_tasks")
    void shouldReturnGenerateTasks() {
        stubCounts(10, 10, 5, 5, 0, 0, 0);
        assertThat(service.getStats(SEMESTER_ID).get("currentStep")).isEqualTo("generate_tasks");
    }

    @Test
    @DisplayName("teacher_assigned 不足时 currentStep=teacher_assign")
    void shouldReturnTeacherAssign() {
        stubCounts(10, 10, 5, 5, 8, 3, 0);
        assertThat(service.getStats(SEMESTER_ID).get("currentStep")).isEqualTo("teacher_assign");
    }

    @Test
    @DisplayName("scheduled<tasks 时 currentStep=scheduling")
    void shouldReturnScheduling() {
        stubCounts(10, 10, 5, 5, 8, 8, 4);
        assertThat(service.getStats(SEMESTER_ID).get("currentStep")).isEqualTo("scheduling");
    }

    @Test
    @DisplayName("所有完成时 currentStep=done, 各段计数正确")
    void shouldReturnDoneAndExposeCounts() {
        stubCounts(10, 10, 5, 5, 8, 8, 8);

        Map<String, Object> result = service.getStats(SEMESTER_ID);

        assertThat(result.get("currentStep")).isEqualTo("done");
        @SuppressWarnings("unchecked")
        Map<String, Object> offerings = (Map<String, Object>) result.get("offerings");
        @SuppressWarnings("unchecked")
        Map<String, Object> tasks = (Map<String, Object>) result.get("tasks");
        assertThat(offerings.get("total")).isEqualTo(10L);
        assertThat(offerings.get("confirmed")).isEqualTo(10L);
        assertThat(tasks.get("total")).isEqualTo(8L);
        assertThat(tasks.get("scheduled")).isEqualTo(8L);
    }
}
