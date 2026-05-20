package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.offering.ClassCourseAssignment;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.offering.SemesterOffering;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.task.TeachingTask;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.ClassCourseAssignmentRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.SemesterOfferingRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.TeachingTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 * OfferingApplicationService 单测 — 验证开课计划/班级任务的领域编排
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OfferingApplicationService 测试")
class OfferingApplicationServiceTest {

    @Mock
    private SemesterOfferingRepository offeringRepo;

    @Mock
    private ClassCourseAssignmentRepository assignmentRepo;

    @Mock
    private TeachingTaskRepository taskRepo;

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private OfferingApplicationService service;

    @Nested
    @DisplayName("listOfferingsWithCourse 开课计划列表")
    class ListOfferingsTests {

        @Test
        @DisplayName("应按 semesterId 组装 JOIN 查询")
        void shouldQueryWithJoin() {
            when(jdbc.queryForList(anyString(), eq(10L)))
                    .thenReturn(Collections.emptyList());

            service.listOfferingsWithCourse(10L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), eq(10L));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("FROM semester_course_offerings o");
            assertThat(sql).contains("LEFT JOIN courses c");
            assertThat(sql).contains("o.semester_id = ?");
        }
    }

    @Nested
    @DisplayName("createOffering 创建开课计划")
    class CreateOfferingTests {

        @Test
        @DisplayName("仅基础字段时直接 create 后 save")
        void shouldCreateWithBasicFields() {
            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 10L);
            data.put("courseId", 7L);
            data.put("applicableGrade", "2024级");
            data.put("orgUnitId", 800L);
            data.put("weeklyHours", 4);
            data.put("endWeek", 16);
            when(offeringRepo.save(any(SemesterOffering.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            SemesterOffering result = service.createOffering(data, 99L);

            assertThat(result.getSemesterId()).isEqualTo(10L);
            assertThat(result.getCourseId()).isEqualTo(7L);
            assertThat(result.getWeeklyHours()).isEqualTo(4);
            assertThat(result.getStartWeek()).isEqualTo(1); // 默认
            verify(offeringRepo).save(any(SemesterOffering.class));
        }

        @Test
        @DisplayName("带可选字段时应额外调用 update 应用扩展配置")
        void shouldApplyOptionalFields() {
            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 10L);
            data.put("courseId", 7L);
            data.put("applicableGrade", "2024级");
            data.put("weeklyHours", 4);
            data.put("endWeek", 16);
            data.put("courseCategory", 2);
            data.put("allowCombined", true);
            data.put("remark", "走班");
            when(offeringRepo.save(any(SemesterOffering.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            SemesterOffering result = service.createOffering(data, 99L);

            assertThat(result.getCourseCategory()).isEqualTo(2);
            assertThat(result.getAllowCombined()).isTrue();
            assertThat(result.getRemark()).isEqualTo("走班");
        }
    }

    @Nested
    @DisplayName("updateOffering 更新开课计划")
    class UpdateOfferingTests {

        @Test
        @DisplayName("不存在时抛出 IllegalArgumentException")
        void shouldThrowWhenNotFound() {
            when(offeringRepo.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateOffering(1L, new HashMap<>()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("开课计划不存在");
        }

        @Test
        @DisplayName("存在时应应用字段并 save")
        void shouldUpdateAndSave() {
            SemesterOffering existing = SemesterOffering.create(
                    10L, 7L, "2024级", 800L, 4, 1, 16, 99L);
            when(offeringRepo.findById(1L)).thenReturn(Optional.of(existing));
            when(offeringRepo.save(any(SemesterOffering.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            Map<String, Object> data = new HashMap<>();
            data.put("weeklyHours", 6);
            data.put("startWeek", 2);
            data.put("endWeek", 18);
            data.put("remark", "更新备注");

            SemesterOffering result = service.updateOffering(1L, data);

            assertThat(result.getWeeklyHours()).isEqualTo(6);
            assertThat(result.getStartWeek()).isEqualTo(2);
            assertThat(result.getRemark()).isEqualTo("更新备注");
            verify(offeringRepo).save(existing);
        }
    }

    @Nested
    @DisplayName("deleteOffering / confirmOffering")
    class DeleteAndConfirmTests {

        @Test
        @DisplayName("deleteOffering 应委托仓储删除")
        void shouldDelete() {
            service.deleteOffering(1L);
            verify(offeringRepo).deleteById(1L);
        }

        @Test
        @DisplayName("confirmOffering 应将草稿确认并 save")
        void shouldConfirm() {
            SemesterOffering existing = SemesterOffering.create(
                    10L, 7L, "2024级", 800L, 4, 1, 16, 99L);
            when(offeringRepo.findById(1L)).thenReturn(Optional.of(existing));
            when(offeringRepo.save(any(SemesterOffering.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.confirmOffering(1L);

            assertThat(existing.getStatus()).isEqualTo(1);
            verify(offeringRepo).save(existing);
        }

        @Test
        @DisplayName("confirmOffering 不存在时抛异常")
        void shouldThrowWhenConfirmNotFound() {
            when(offeringRepo.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.confirmOffering(1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("listAssignments 班级任务列表")
    class ListAssignmentsTests {

        @Test
        @DisplayName("orgUnitId 非空时按班级查询")
        void shouldQueryByClassWhenOrgUnitProvided() {
            when(assignmentRepo.findBySemesterIdAndClassId(10L, 800L))
                    .thenReturn(Collections.emptyList());

            service.listAssignments(10L, 800L);

            verify(assignmentRepo).findBySemesterIdAndClassId(10L, 800L);
            verify(assignmentRepo, never()).findBySemesterId(anyLong());
        }

        @Test
        @DisplayName("orgUnitId 为空时按学期查询")
        void shouldQueryBySemesterWhenOrgUnitNull() {
            when(assignmentRepo.findBySemesterId(10L))
                    .thenReturn(Collections.emptyList());

            service.listAssignments(10L, null);

            verify(assignmentRepo).findBySemesterId(10L);
        }
    }

    @Nested
    @DisplayName("createAssignment / deleteAssignment")
    class AssignmentCrudTests {

        @Test
        @DisplayName("createAssignment 应构造实体并 save")
        void shouldCreateAssignment() {
            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 10L);
            data.put("orgUnitId", 800L);
            data.put("offeringId", 500L);
            data.put("courseId", 7L);
            data.put("weeklyHours", 4);
            data.put("studentCount", 30);
            when(assignmentRepo.save(any(ClassCourseAssignment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ClassCourseAssignment result = service.createAssignment(data, 99L);

            assertThat(result.getSemesterId()).isEqualTo(10L);
            assertThat(result.getOfferingId()).isEqualTo(500L);
            assertThat(result.getStudentCount()).isEqualTo(30);
            assertThat(result.getStatus()).isZero();
        }

        @Test
        @DisplayName("deleteAssignment 应委托仓储删除")
        void shouldDeleteAssignment() {
            service.deleteAssignment(3L);
            verify(assignmentRepo).deleteById(3L);
        }
    }

    @Nested
    @DisplayName("batchConfirmAssignments 批量确认")
    class BatchConfirmTests {

        @Test
        @DisplayName("应确认所有班级任务并逐个 save")
        void shouldConfirmAll() {
            ClassCourseAssignment a1 = ClassCourseAssignment.create(10L, 800L, 500L, 7L, 4, 30, 99L);
            ClassCourseAssignment a2 = ClassCourseAssignment.create(10L, 801L, 501L, 8L, 2, 25, 99L);
            when(assignmentRepo.findBySemesterIdAndClassId(10L, 800L))
                    .thenReturn(List.of(a1, a2));

            service.batchConfirmAssignments(10L, 800L);

            assertThat(a1.getStatus()).isEqualTo(1);
            assertThat(a2.getStatus()).isEqualTo(1);
            verify(assignmentRepo, times(2)).save(any(ClassCourseAssignment.class));
        }

        @Test
        @DisplayName("无任务时不调用 save")
        void shouldNotSaveWhenEmpty() {
            when(assignmentRepo.findBySemesterIdAndClassId(10L, 800L))
                    .thenReturn(Collections.emptyList());

            service.batchConfirmAssignments(10L, 800L);

            verify(assignmentRepo, never()).save(any(ClassCourseAssignment.class));
        }
    }

    @Nested
    @DisplayName("importFromPlan 从培养方案导入")
    class ImportFromPlanTests {

        private Map<String, Object> planCourse(long courseId, Integer weeklyHours) {
            Map<String, Object> m = new HashMap<>();
            m.put("course_id", courseId);
            m.put("weekly_hours", weeklyHours);
            m.put("total_hours", 64);
            m.put("course_category", 1);
            m.put("course_type", 1);
            return m;
        }

        @Test
        @DisplayName("应为未存在的课程创建开课计划")
        void shouldCreateOfferingsFromPlan() {
            Map<String, Object> plan = new HashMap<>();
            plan.put("grade_year", "2024");
            when(jdbc.queryForMap(anyString(), eq(500L))).thenReturn(plan);
            when(jdbc.queryForList(anyString(), eq(500L)))
                    .thenReturn(List.of(planCourse(7L, 4), planCourse(8L, 2)));
            when(offeringRepo.findBySemesterId(10L)).thenReturn(Collections.emptyList());

            int created = service.importFromPlan(10L, 500L, 99L);

            assertThat(created).isEqualTo(2);
            verify(offeringRepo, times(2)).save(any(SemesterOffering.class));
        }

        @Test
        @DisplayName("已存在的 course+grade 应跳过")
        void shouldSkipExistingOffering() {
            Map<String, Object> plan = new HashMap<>();
            plan.put("grade_year", "2024");
            when(jdbc.queryForMap(anyString(), eq(500L))).thenReturn(plan);
            when(jdbc.queryForList(anyString(), eq(500L)))
                    .thenReturn(List.of(planCourse(7L, 4)));
            SemesterOffering existing = SemesterOffering.create(
                    10L, 7L, "2024级", null, 4, 1, 16, 99L);
            when(offeringRepo.findBySemesterId(10L)).thenReturn(List.of(existing));

            int created = service.importFromPlan(10L, 500L, 99L);

            assertThat(created).isZero();
            verify(offeringRepo, never()).save(any(SemesterOffering.class));
        }

        @Test
        @DisplayName("grade_year 为 null 时年级回退为 '全年级'")
        void shouldFallbackToAllGradesWhenPlanGradeNull() {
            Map<String, Object> plan = new HashMap<>();
            plan.put("grade_year", null);
            when(jdbc.queryForMap(anyString(), eq(500L))).thenReturn(plan);
            when(jdbc.queryForList(anyString(), eq(500L)))
                    .thenReturn(List.of(planCourse(7L, null)));
            when(offeringRepo.findBySemesterId(10L)).thenReturn(Collections.emptyList());
            ArgumentCaptor<SemesterOffering> cap = ArgumentCaptor.forClass(SemesterOffering.class);

            int created = service.importFromPlan(10L, 500L, 99L);

            assertThat(created).isEqualTo(1);
            verify(offeringRepo).save(cap.capture());
            assertThat(cap.getValue().getApplicableGrade()).isEqualTo("全年级");
            // weekly_hours 为 null → 默认 2
            assertThat(cap.getValue().getWeeklyHours()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("generateTasksFromAssignments 从班级任务生成教学任务")
    class GenerateTasksTests {

        private ClassCourseAssignment confirmedAssignment(Long orgUnitId, Long courseId) {
            ClassCourseAssignment a = ClassCourseAssignment.create(
                    10L, orgUnitId, 500L, courseId, 4, 30, 99L);
            a.confirm();
            return a;
        }

        @Test
        @DisplayName("无已确认任务时返回 0")
        void shouldReturnZeroWhenNoConfirmedAssignments() {
            ClassCourseAssignment pending = ClassCourseAssignment.create(
                    10L, 800L, 500L, 7L, 4, 30, 99L); // status=0
            when(assignmentRepo.findBySemesterId(10L)).thenReturn(List.of(pending));

            int created = service.generateTasksFromAssignments(10L, 99L);

            assertThat(created).isZero();
            verify(taskRepo, never()).save(any(TeachingTask.class));
        }

        @Test
        @DisplayName("应为已确认任务生成教学任务")
        void shouldGenerateTask() {
            ClassCourseAssignment a = confirmedAssignment(800L, 7L);
            when(assignmentRepo.findBySemesterId(10L)).thenReturn(List.of(a));
            SemesterOffering offering = SemesterOffering.create(
                    10L, 7L, "2024级", 800L, 4, 1, 16, 99L);
            when(offeringRepo.findById(500L)).thenReturn(Optional.of(offering));
            Map<String, Object> studentCountRow = new HashMap<>();
            studentCountRow.put("org_unit_id", 800L);
            studentCountRow.put("cnt", 28);
            // 两次 queryForList 走同一个 varargs 方法, 按 SQL 内容区分:
            // teaching_tasks 查重返回空, user_student 计数返回行
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenAnswer(inv -> {
                        String sql = inv.getArgument(0);
                        return sql.contains("user_student")
                                ? List.of(studentCountRow)
                                : Collections.emptyList();
                    });
            when(taskRepo.save(any(TeachingTask.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            int created = service.generateTasksFromAssignments(10L, 99L);

            assertThat(created).isEqualTo(1);
            ArgumentCaptor<TeachingTask> cap = ArgumentCaptor.forClass(TeachingTask.class);
            verify(taskRepo).save(cap.capture());
            assertThat(cap.getValue().getCourseId()).isEqualTo(7L);
            assertThat(cap.getValue().getStudentCount()).isEqualTo(28);
        }

        @Test
        @DisplayName("已存在 course+org 的 task 应跳过")
        void shouldSkipExistingTask() {
            ClassCourseAssignment a = confirmedAssignment(800L, 7L);
            when(assignmentRepo.findBySemesterId(10L)).thenReturn(List.of(a));
            Map<String, Object> existing = new HashMap<>();
            existing.put("course_id", 7L);
            existing.put("org_unit_id", 800L);
            SemesterOffering offering = SemesterOffering.create(
                    10L, 7L, "2024级", 800L, 4, 1, 16, 99L);
            when(offeringRepo.findById(500L)).thenReturn(Optional.of(offering));
            Map<String, Object> studentCountRow = new HashMap<>();
            studentCountRow.put("org_unit_id", 800L);
            studentCountRow.put("cnt", 28);
            // 按 SQL 内容区分: teaching_tasks 查重返回已存在行, user_student 计数返回行
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenAnswer(inv -> {
                        String sql = inv.getArgument(0);
                        return sql.contains("user_student")
                                ? List.of(studentCountRow)
                                : List.of(existing);
                    });

            int created = service.generateTasksFromAssignments(10L, 99L);

            assertThat(created).isZero();
            verify(taskRepo, never()).save(any(TeachingTask.class));
        }

        @Test
        @DisplayName("学生数查询异常时仍生成任务且 studentCount=0")
        void shouldGenerateWithZeroStudentsWhenQueryFails() {
            ClassCourseAssignment a = confirmedAssignment(800L, 7L);
            when(assignmentRepo.findBySemesterId(10L)).thenReturn(List.of(a));
            SemesterOffering offering = SemesterOffering.create(
                    10L, 7L, "2024级", 800L, 4, 1, 16, 99L);
            when(offeringRepo.findById(500L)).thenReturn(Optional.of(offering));
            // teaching_tasks 查重返回空; 仅 user_student 计数查询抛异常 (服务在 try/catch 内吞掉)
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenAnswer(inv -> {
                        String sql = inv.getArgument(0);
                        if (sql.contains("user_student")) {
                            throw new RuntimeException("no user_student");
                        }
                        return Collections.emptyList();
                    });
            when(taskRepo.save(any(TeachingTask.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            int created = service.generateTasksFromAssignments(10L, 99L);

            assertThat(created).isEqualTo(1);
            ArgumentCaptor<TeachingTask> cap = ArgumentCaptor.forClass(TeachingTask.class);
            verify(taskRepo).save(cap.capture());
            assertThat(cap.getValue().getStudentCount()).isZero();
        }
    }
}
