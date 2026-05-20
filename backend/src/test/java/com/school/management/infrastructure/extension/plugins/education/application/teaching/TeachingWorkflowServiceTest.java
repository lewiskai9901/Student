package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.school.management.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TeachingWorkflowService 单测 — 验证教务工作流编排的 JDBC 行为
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeachingWorkflowService 测试")
class TeachingWorkflowServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private TeachingWorkflowService service;

    private Map<String, Object> semesterRow(int startYear, int semType) {
        Map<String, Object> m = new HashMap<>();
        m.put("start_year", startYear);
        m.put("semester_type", semType);
        return m;
    }

    private Map<String, Object> cohortRow(long id, Integer enrollYear, Integer gradYear) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("enrollment_year", enrollYear);
        m.put("graduation_year", gradYear);
        return m;
    }

    @Nested
    @DisplayName("generateCohortSemesterMappings 年级-学期映射")
    class GenerateCohortSemesterMappingsTests {

        @BeforeEach
        void semester() {
            // 学期 2024 秋季 (semester_type=1)
            when(jdbc.queryForMap(anyString(), eq(10L))).thenReturn(semesterRow(2024, 1));
        }

        @Test
        @DisplayName("应为有效年级插入映射并返回插入条数")
        void shouldInsertMappingForValidCohort() {
            // 2024 入学年级 → programSem = (2024-2024)*2+1 = 1, 有效
            when(jdbc.queryForList(anyString()))
                    .thenReturn(List.of(cohortRow(100L, 2024, 2027)));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(100L), eq(10L)))
                    .thenReturn(0L); // 不存在已映射
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(2024)))
                    .thenReturn(500L); // 培养方案 id
            when(jdbc.update(anyString(), any(), any(), any(), any())).thenReturn(1);

            int n = service.generateCohortSemesterMappings(10L);

            assertThat(n).isEqualTo(1);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(100L), eq(10L), eq(1), eq(500L));
            assertThat(sqlCap.getValue()).contains("INSERT INTO cohort_semester_mapping");
        }

        @Test
        @DisplayName("已存在映射的年级应跳过不插入")
        void shouldSkipWhenMappingExists() {
            when(jdbc.queryForList(anyString()))
                    .thenReturn(List.of(cohortRow(100L, 2024, 2027)));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(100L), eq(10L)))
                    .thenReturn(3L); // 已存在

            int n = service.generateCohortSemesterMappings(10L);

            assertThat(n).isZero();
            verify(jdbc, never()).update(anyString(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("programSemester 超出修业范围的年级应跳过")
        void shouldSkipWhenProgramSemesterOutOfRange() {
            // 2010 入学年级 → programSem = (2024-2010)*2+1 = 29, 远超 3*2=6
            when(jdbc.queryForList(anyString()))
                    .thenReturn(List.of(cohortRow(100L, 2010, 2013)));

            int n = service.generateCohortSemesterMappings(10L);

            assertThat(n).isZero();
            verify(jdbc, never()).queryForObject(anyString(), eq(Long.class), anyLong(), anyLong());
        }

        @Test
        @DisplayName("培养方案查询异常时 planId 兜底为 null 仍插入")
        void shouldInsertWithNullPlanWhenPlanQueryFails() {
            when(jdbc.queryForList(anyString()))
                    .thenReturn(List.of(cohortRow(100L, 2024, null)));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(100L), eq(10L)))
                    .thenReturn(0L);
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(2024)))
                    .thenThrow(new RuntimeException("no plan"));
            when(jdbc.update(anyString(), any(), any(), any(), any())).thenReturn(1);

            int n = service.generateCohortSemesterMappings(10L);

            assertThat(n).isEqualTo(1);
            verify(jdbc).update(anyString(), eq(100L), eq(10L), eq(1), eq((Long) null));
        }

        @Test
        @DisplayName("无年级时返回 0")
        void shouldReturnZeroWhenNoCohorts() {
            when(jdbc.queryForList(anyString())).thenReturn(Collections.emptyList());

            int n = service.generateCohortSemesterMappings(10L);

            assertThat(n).isZero();
        }
    }

    @Nested
    @DisplayName("generateOfferingsFromMappings 开课计划生成")
    class GenerateOfferingsFromMappingsTests {

        private Map<String, Object> mappingRow() {
            Map<String, Object> m = new HashMap<>();
            m.put("cohort_id", 100L);
            m.put("program_semester", 1);
            m.put("plan_id", 500L);
            m.put("grade_name", "2024级");
            return m;
        }

        private Map<String, Object> planCourseRow(long courseId) {
            Map<String, Object> m = new HashMap<>();
            m.put("course_id", courseId);
            m.put("weekly_hours", 4);
            m.put("course_category", 1);
            m.put("course_type", 1);
            m.put("plan_course_id", 9L);
            return m;
        }

        @Test
        @DisplayName("应为培养方案课程插入开课计划")
        void shouldInsertOffering() {
            when(jdbc.queryForList(anyString(), eq(10L)))
                    .thenReturn(List.of(mappingRow()))   // mappings
                    .thenReturn(Collections.emptyList()); // existing offerings
            when(jdbc.queryForList(anyString(), eq(500L), eq(1)))
                    .thenReturn(List.of(planCourseRow(7L)));
            when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(1);

            int n = service.generateOfferingsFromMappings(10L, 99L);

            assertThat(n).isEqualTo(1);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(),
                    eq(10L), eq(500L), eq(9L), eq(7L),
                    eq("2024级"), eq(4), eq(1), eq(1), eq(99L));
            assertThat(sqlCap.getValue()).contains("INSERT INTO semester_course_offerings");
        }

        @Test
        @DisplayName("已存在 course+grade 组合应跳过")
        void shouldSkipExistingOffering() {
            Map<String, Object> existing = new HashMap<>();
            existing.put("course_id", 7L);
            existing.put("applicable_grade", "2024级");
            when(jdbc.queryForList(anyString(), eq(10L)))
                    .thenReturn(List.of(mappingRow()))
                    .thenReturn(List.of(existing));
            when(jdbc.queryForList(anyString(), eq(500L), eq(1)))
                    .thenReturn(List.of(planCourseRow(7L)));

            int n = service.generateOfferingsFromMappings(10L, 99L);

            assertThat(n).isZero();
            verify(jdbc, never()).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("无映射时返回 0")
        void shouldReturnZeroWhenNoMappings() {
            when(jdbc.queryForList(anyString(), eq(10L)))
                    .thenReturn(Collections.emptyList())
                    .thenReturn(Collections.emptyList());

            int n = service.generateOfferingsFromMappings(10L, 99L);

            assertThat(n).isZero();
        }
    }

    @Nested
    @DisplayName("generateTasksFromOfferings 教学任务生成")
    class GenerateTasksFromOfferingsTests {

        private Map<String, Object> assignmentRow() {
            Map<String, Object> m = new HashMap<>();
            m.put("assignmentId", 1L);
            m.put("offeringId", 500L);
            m.put("courseId", 7L);
            m.put("orgUnitId", 800L);
            m.put("weeklyHours", 4);
            m.put("studentCount", 30);
            m.put("offeringStartWeek", 1);
            m.put("offeringEndWeek", 16);
            m.put("offeringWeekType", 0);
            return m;
        }

        @Test
        @DisplayName("应为开课计划生成教学任务并算出总学时")
        void shouldInsertTaskWithTotalHours() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(10L)))
                    .thenReturn(16); // teaching weeks
            when(jdbc.queryForList(anyString(), eq(10L)))
                    .thenReturn(List.of(assignmentRow()));
            when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

            int n = service.generateTasksFromOfferings(10L, 99L);

            assertThat(n).isEqualTo(1);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            // totalHours = 4 * (16-1+1) = 64
            verify(jdbc).update(sqlCap.capture(),
                    any(), any(), eq(10L), eq(7L), eq(800L), eq(500L),
                    eq(30), eq(4), eq(64), eq(1), eq(16), eq(0), eq(99L));
            assertThat(sqlCap.getValue()).contains("INSERT INTO teaching_tasks");
        }

        @Test
        @DisplayName("单双周课程总学时应减半")
        void shouldHalveTotalHoursForOddEvenWeek() {
            Map<String, Object> row = assignmentRow();
            row.put("offeringWeekType", 1); // 单周
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(10L)))
                    .thenReturn(16);
            when(jdbc.queryForList(anyString(), eq(10L))).thenReturn(List.of(row));
            when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

            int n = service.generateTasksFromOfferings(10L, 99L);

            assertThat(n).isEqualTo(1);
            // totalHours = round(64 / 2) = 32
            verify(jdbc).update(anyString(),
                    any(), any(), eq(10L), eq(7L), eq(800L), eq(500L),
                    eq(30), eq(4), eq(32), eq(1), eq(16), eq(1), eq(99L));
        }

        @Test
        @DisplayName("无开课计划时返回 0")
        void shouldReturnZeroWhenNoAssignments() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(10L)))
                    .thenReturn(16);
            when(jdbc.queryForList(anyString(), eq(10L)))
                    .thenReturn(Collections.emptyList());

            int n = service.generateTasksFromOfferings(10L, 99L);

            assertThat(n).isZero();
        }

        @Test
        @DisplayName("教学周查询异常时兜底为 16 周")
        void shouldFallbackTo16WeeksWhenWeekQueryFails() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(10L)))
                    .thenThrow(new RuntimeException("no academic_weeks"));
            Map<String, Object> row = assignmentRow();
            row.put("offeringEndWeek", null); // 用兜底 endWeek
            when(jdbc.queryForList(anyString(), eq(10L))).thenReturn(List.of(row));
            when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

            int n = service.generateTasksFromOfferings(10L, 99L);

            assertThat(n).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("generateExamFromTasks 考试安排生成")
    class GenerateExamFromTasksTests {

        private Map<String, Object> batchRow() {
            Map<String, Object> m = new HashMap<>();
            m.put("batch_name", "2024期末考");
            m.put("start_date", "2024-12-20");
            m.put("end_date", "2024-12-30");
            m.put("exam_type", 2);
            return m;
        }

        private Map<String, Object> taskRow(long id) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("course_id", 7L);
            m.put("org_unit_id", 800L);
            m.put("student_count", 30);
            m.put("course_exam_form", 1);
            m.put("course_hours", 64);
            return m;
        }

        @Test
        @DisplayName("taskIds 为空应抛出 BusinessException")
        void shouldThrowWhenTaskIdsEmpty() {
            assertThatThrownBy(() -> service.generateExamFromTasks(5L, Collections.emptyList(), 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("请选择教学任务");
        }

        @Test
        @DisplayName("taskIds 为 null 应抛出 BusinessException")
        void shouldThrowWhenTaskIdsNull() {
            assertThatThrownBy(() -> service.generateExamFromTasks(5L, null, 99L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("应为每个 task 插入考试安排")
        void shouldInsertExamArrangement() {
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(batchRow());
            when(jdbc.queryForList(anyString(), eq(new Object[]{1L})))
                    .thenReturn(List.of(taskRow(1L)));
            when(jdbc.queryForList(anyString(), eq(5L)))
                    .thenReturn(Collections.emptyList()); // 无已存在
            when(jdbc.update(anyString(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any())).thenReturn(1);

            int n = service.generateExamFromTasks(5L, List.of(1L), 99L);

            assertThat(n).isEqualTo(1);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(),
                    eq(5L), eq(7L), eq(1L), eq(800L), eq("2024-12-20"),
                    any(), any(), eq(1), eq(30), eq(99L));
            assertThat(sqlCap.getValue()).contains("INSERT INTO exam_arrangements");
        }

        @Test
        @DisplayName("已存在的 task 应跳过不重复插入")
        void shouldSkipExistingExam() {
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(batchRow());
            when(jdbc.queryForList(anyString(), eq(new Object[]{1L})))
                    .thenReturn(List.of(taskRow(1L)));
            Map<String, Object> existing = new HashMap<>();
            existing.put("task_id", 1L);
            when(jdbc.queryForList(anyString(), eq(5L))).thenReturn(List.of(existing));

            int n = service.generateExamFromTasks(5L, List.of(1L), 99L);

            assertThat(n).isZero();
            verify(jdbc, never()).update(anyString(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("task 不存在于库中应跳过")
        void shouldSkipMissingTask() {
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(batchRow());
            when(jdbc.queryForList(anyString(), eq(new Object[]{1L})))
                    .thenReturn(Collections.emptyList()); // task 不存在
            when(jdbc.queryForList(anyString(), eq(5L)))
                    .thenReturn(Collections.emptyList());

            int n = service.generateExamFromTasks(5L, List.of(1L), 99L);

            assertThat(n).isZero();
        }
    }

    @Nested
    @DisplayName("generateGradeBatchFromExam 成绩批次生成")
    class GenerateGradeBatchFromExamTests {

        private Map<String, Object> examBatchRow(int examType) {
            Map<String, Object> m = new HashMap<>();
            m.put("batch_name", "期末考");
            m.put("semester_id", 10L);
            m.put("exam_type", examType);
            return m;
        }

        private Map<String, Object> arrangementRow() {
            Map<String, Object> m = new HashMap<>();
            m.put("task_id", 1L);
            m.put("course_id", 7L);
            m.put("org_unit_id", 800L);
            return m;
        }

        private Map<String, Object> studentRow(long id) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("org_unit_id", 800L);
            return m;
        }

        @Test
        @DisplayName("应创建成绩批次并批量插入待录入成绩")
        void shouldCreateGradeBatchAndInsertGrades() {
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(examBatchRow(2));
            when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
            when(jdbc.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class)))
                    .thenReturn(900L);
            // 三次 queryForList 走同一 varargs 方法, 按 SQL 内容区分
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenAnswer(inv -> {
                        String sql = inv.getArgument(0);
                        if (sql.contains("exam_arrangements")) return List.of(arrangementRow());
                        if (sql.contains("student_grades")) return Collections.emptyList();
                        return List.of(studentRow(1001L), studentRow(1002L)); // user_student
                    });
            when(jdbc.batchUpdate(anyString(), any(List.class)))
                    .thenReturn(new int[]{1, 1});

            Long gradeBatchId = service.generateGradeBatchFromExam(5L, 99L);

            assertThat(gradeBatchId).isEqualTo(900L);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(), any(), eq(10L), eq(3), eq(99L));
            assertThat(sqlCap.getValue()).contains("INSERT INTO grade_batches");
            verify(jdbc).batchUpdate(anyString(), any(List.class));
        }

        @Test
        @DisplayName("期中考(examType=1)应映射为 gradeType=2")
        void shouldMapMidtermToGradeType2() {
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(examBatchRow(1));
            when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
            when(jdbc.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class)))
                    .thenReturn(900L);
            when(jdbc.queryForList(anyString(), eq(5L)))
                    .thenReturn(Collections.emptyList());
            when(jdbc.queryForList(anyString(), eq(900L), eq(10L)))
                    .thenReturn(Collections.emptyList());

            service.generateGradeBatchFromExam(5L, 99L);

            verify(jdbc).update(anyString(), any(), any(), eq(10L), eq(2), eq(99L));
        }

        @Test
        @DisplayName("无考试安排时不执行 batchUpdate")
        void shouldNotBatchUpdateWhenNoArrangements() {
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(examBatchRow(2));
            when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
            when(jdbc.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class)))
                    .thenReturn(900L);
            when(jdbc.queryForList(anyString(), eq(5L)))
                    .thenReturn(Collections.emptyList());
            when(jdbc.queryForList(anyString(), eq(900L), eq(10L)))
                    .thenReturn(Collections.emptyList());

            Long gradeBatchId = service.generateGradeBatchFromExam(5L, 99L);

            assertThat(gradeBatchId).isEqualTo(900L);
            verify(jdbc, never()).batchUpdate(anyString(), any(List.class));
        }

        @Test
        @DisplayName("学生表查询异常时仍返回成绩批次id, 不插入成绩")
        void shouldStillReturnBatchIdWhenStudentQueryFails() {
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(examBatchRow(2));
            when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
            when(jdbc.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class)))
                    .thenReturn(900L);
            // 仅 user_student 查询抛异常 (服务在 try/catch 内吞掉), 其余正常返回
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenAnswer(inv -> {
                        String sql = inv.getArgument(0);
                        if (sql.contains("exam_arrangements")) return List.of(arrangementRow());
                        if (sql.contains("student_grades")) return Collections.emptyList();
                        throw new RuntimeException("no user_student");
                    });

            Long gradeBatchId = service.generateGradeBatchFromExam(5L, 99L);

            assertThat(gradeBatchId).isEqualTo(900L);
            verify(jdbc, never()).batchUpdate(anyString(), any(List.class));
        }

        @Test
        @DisplayName("已存在 student+course 成绩应跳过")
        void shouldSkipExistingGrade() {
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(examBatchRow(2));
            when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
            when(jdbc.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class)))
                    .thenReturn(900L);
            Map<String, Object> existingGrade = new HashMap<>();
            existingGrade.put("student_id", 1001L);
            existingGrade.put("course_id", 7L);
            // student_grades 查重返回已存在行 → 学生 1001+课程 7 应被跳过
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenAnswer(inv -> {
                        String sql = inv.getArgument(0);
                        if (sql.contains("exam_arrangements")) return List.of(arrangementRow());
                        if (sql.contains("student_grades")) return List.of(existingGrade);
                        return List.of(studentRow(1001L)); // user_student
                    });

            Long gradeBatchId = service.generateGradeBatchFromExam(5L, 99L);

            assertThat(gradeBatchId).isEqualTo(900L);
            verify(jdbc, never()).batchUpdate(anyString(), any(List.class));
        }
    }
}
