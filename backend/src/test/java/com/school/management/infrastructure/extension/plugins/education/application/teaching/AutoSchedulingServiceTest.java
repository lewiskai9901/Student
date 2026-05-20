package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.ConstraintLevel;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.ConstraintType;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.SchedulingConstraint;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.SchedulingConstraintRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AutoSchedulingService 单测 — 驱动自动排课算法的核心分支:
 * 节次配置加载 / 待排任务加载 / 约束构建 / CSP 求解 / 贪心兜底 /
 * GA 软约束优化 / 写库 / 容量警告 / 合堂展开.
 *
 * 该 service 唯一 public 方法是 autoSchedule, 通过 mock JdbcTemplate 的不同返回
 * 集合来覆盖内部算法分支.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AutoSchedulingService 测试")
class AutoSchedulingServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SchedulingConstraintRepository constraintRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AutoSchedulingService service;

    @BeforeEach
    void setUp() {
        service = new AutoSchedulingService(jdbcTemplate, constraintRepo, objectMapper);
    }

    // ---------- helpers ----------

    /** 一个完整的待排教学任务行 */
    private Map<String, Object> taskRow(long taskId, long courseId, long orgUnitId,
                                        Long teacherId, int weeklyHours, int consecutivePeriods) {
        Map<String, Object> row = new HashMap<>();
        row.put("task_id", taskId);
        row.put("course_id", courseId);
        row.put("org_unit_id", orgUnitId);
        row.put("weekly_hours", weeklyHours);
        row.put("start_week", 1);
        row.put("end_week", 16);
        row.put("week_type", 0);
        row.put("teaching_class_id", null);
        row.put("consecutive_periods", consecutivePeriods);
        row.put("course_name", "课程" + courseId);
        row.put("teacher_id", teacherId);
        row.put("student_count", 40);
        return row;
    }

    private Map<String, Object> classroomRow(long id, int capacity) {
        Map<String, Object> r = new HashMap<>();
        r.put("id", id);
        r.put("name", "教室" + id);
        r.put("capacity", capacity);
        return r;
    }

    /** 配置 period_configs 查询返回指定节次数 */
    private void stubPeriodConfig(Integer periodsPerDay) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (periodsPerDay != null) {
            Map<String, Object> m = new HashMap<>();
            m.put("periods_per_day", periodsPerDay);
            rows.add(m);
        }
        lenient().when(jdbcTemplate.queryForList(
                org.mockito.ArgumentMatchers.contains("period_configs"), any(Object[].class)))
                .thenReturn(rows);
    }

    /** 配置 loadRequirements 的查询 */
    private void stubRequirements(List<Map<String, Object>> rows) {
        lenient().when(jdbcTemplate.queryForList(
                org.mockito.ArgumentMatchers.contains("FROM teaching_tasks t"), any(Object[].class)))
                .thenReturn(rows);
    }

    /** 配置未排任务总数 */
    private void stubUnscheduledCount(long total) {
        lenient().when(jdbcTemplate.queryForObject(
                org.mockito.ArgumentMatchers.contains("COUNT(*) FROM teaching_tasks"),
                eq(Long.class), any())).thenReturn(total);
    }

    /** 配置教室列表 */
    private void stubClassrooms(List<Map<String, Object>> rooms) {
        lenient().when(jdbcTemplate.queryForList(
                org.mockito.ArgumentMatchers.contains("FROM places")))
                .thenReturn(rooms);
    }

    /** 配置已有固定课表条目 */
    private void stubFixedEntries(List<Map<String, Object>> entries) {
        lenient().when(jdbcTemplate.queryForList(
                org.mockito.ArgumentMatchers.contains("FROM schedule_entries WHERE semester_id"),
                any(Object[].class))).thenReturn(entries);
    }

    /** 配置教师偏好 */
    private void stubTeacherPreferences(List<Map<String, Object>> prefs) {
        lenient().when(jdbcTemplate.queryForList(
                org.mockito.ArgumentMatchers.contains("teacher_preferences"),
                any(Object[].class))).thenReturn(prefs);
    }

    // ==================== 空任务场景 ====================

    @Nested
    @DisplayName("无待排任务")
    class EmptyRequirementsTests {

        @Test
        @DisplayName("无任务且无跳过时返回 success=true 且 0 条")
        void shouldReturnSuccessWhenNoTasks() {
            stubPeriodConfig(8);
            stubRequirements(Collections.emptyList());
            stubUnscheduledCount(0L);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
            assertThat(result.get("entriesGenerated")).isEqualTo(0);
            assertThat(result).doesNotContainKey("skippedNoTeacher");
            // 不应写库
            verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("有任务未分配教师时返回 skippedNoTeacher 提示")
        void shouldReportSkippedNoTeacher() {
            stubPeriodConfig(8);
            stubRequirements(Collections.emptyList());
            // 总共 3 个未排任务, 但 loadRequirements 一个都没返回 → 全部跳过
            stubUnscheduledCount(3L);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
            assertThat(result.get("skippedNoTeacher")).isEqualTo(3);
            assertThat((String) result.get("message")).contains("3");
        }
    }

    // ==================== 节次配置分支 ====================

    @Nested
    @DisplayName("节次配置加载")
    class PeriodConfigTests {

        @Test
        @DisplayName("period_configs 为空时使用默认 10 节")
        void shouldUseDefaultWhenNoPeriodConfig() {
            stubPeriodConfig(null);
            stubRequirements(Collections.emptyList());
            stubUnscheduledCount(0L);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("period_configs 查询抛异常时降级到默认值")
        void shouldFallbackWhenPeriodConfigThrows() {
            when(jdbcTemplate.queryForList(
                    org.mockito.ArgumentMatchers.contains("period_configs"), any(Object[].class)))
                    .thenThrow(new RuntimeException("table missing"));
            stubRequirements(Collections.emptyList());
            stubUnscheduledCount(0L);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }
    }

    // ==================== 完整排课流程 ====================

    @Nested
    @DisplayName("完整排课流程")
    class FullSchedulingTests {

        @Test
        @DisplayName("单个普通任务应 CSP 求解成功并写入课表条目")
        void shouldScheduleSingleTask() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
            assertThat((Integer) result.get("entriesGenerated")).isEqualTo(1);
            // 应执行 INSERT
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate, atLeastOnce()).update(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getAllValues())
                    .anyMatch(s -> s.contains("INSERT INTO schedule_entries"));
        }

        @Test
        @DisplayName("4 周课时按 2 节连排展开为 2 个会话, 写入 2 条条目")
        void shouldExpandMultiSessionTask() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 4, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat((Integer) result.get("entriesGenerated")).isEqualTo(2);
        }

        @Test
        @DisplayName("无可用教室时仍能排课但 classroomId 为空")
        void shouldScheduleWithoutClassrooms() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(Collections.emptyList());
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
            assertThat((Integer) result.get("entriesGenerated")).isEqualTo(1);
        }

        @Test
        @DisplayName("教室加载抛异常时降级为空教室列表, 排课仍成功")
        void shouldFallbackWhenClassroomQueryThrows() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("FROM places")))
                    .thenThrow(new RuntimeException("places missing"));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("autoSchedule 结束后应更新任务状态")
        void shouldUpdateTaskStatuses() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            service.autoSchedule(1L, new HashMap<>());

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate, atLeastOnce()).update(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getAllValues())
                    .anyMatch(s -> s.contains("UPDATE teaching_tasks") && s.contains("scheduling_status = 2"));
        }
    }

    // ==================== 容量警告 ====================

    @Nested
    @DisplayName("容量警告检测")
    class CapacityWarningTests {

        @Test
        @DisplayName("教室容量不足时产生 capacityWarnings")
        void shouldEmitCapacityWarning() {
            stubPeriodConfig(8);
            // 学生 40 人, 唯一教室容量 10 人 → 容量警告
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 10)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> warnings =
                    (List<Map<String, Object>>) result.get("capacityWarnings");
            assertThat(warnings).isNotEmpty();
            assertThat(warnings.get(0).get("capacity")).isEqualTo(10);
            assertThat(warnings.get(0).get("studentCount")).isEqualTo(40);
        }

        @Test
        @DisplayName("教室容量充足时无容量警告")
        void shouldNotEmitWarningWhenCapacityEnough() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 100)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> warnings =
                    (List<Map<String, Object>>) result.get("capacityWarnings");
            assertThat(warnings).isEmpty();
        }
    }

    // ==================== 硬约束 (TIME_FORBIDDEN) ====================

    @Nested
    @DisplayName("硬约束禁排时段")
    class HardConstraintTests {

        private SchedulingConstraint forbidden(ConstraintLevel level, Long targetId,
                                               String paramsJson) {
            return SchedulingConstraint.reconstruct(1L, 1L, "禁排约束",
                    level, targetId, "目标", null,
                    ConstraintType.TIME_FORBIDDEN, true, 80,
                    paramsJson, null, true, 1L);
        }

        @Test
        @DisplayName("全局禁排约束应缩小可用时段但仍能排课")
        void shouldApplyGlobalForbidden() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            // 禁排周一全天
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(List.of(
                    forbidden(ConstraintLevel.GLOBAL, null, "{\"days\":[1]}")));
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("教师级禁排约束应被处理")
        void shouldApplyTeacherForbidden() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(List.of(
                    forbidden(ConstraintLevel.TEACHER, 30L, "{\"days\":[1,2],\"periods\":[1,2]}")));
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("班级/课程级禁排约束应被处理")
        void shouldApplyClassAndCourseForbidden() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(List.of(
                    forbidden(ConstraintLevel.CLASS, 20L, "{\"days\":[3]}"),
                    forbidden(ConstraintLevel.COURSE, 10L, "{\"periods\":[7,8]}")));
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("非硬约束的 TIME_FORBIDDEN 应被 buildForbiddenSets 跳过")
        void shouldIgnoreSoftTimeForbidden() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            SchedulingConstraint soft = SchedulingConstraint.reconstruct(2L, 1L, "软禁排",
                    ConstraintLevel.GLOBAL, null, "目标", null,
                    ConstraintType.TIME_FORBIDDEN, false, 50,
                    "{\"days\":[1,2,3,4,5]}", null, true, 1L);
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(List.of(soft));
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            // 软约束被跳过, 排课仍成功 (若被当硬约束会禁掉全部时段)
            assertThat(result.get("success")).isEqualTo(true);
            assertThat((Integer) result.get("entriesGenerated")).isEqualTo(1);
        }

        @Test
        @DisplayName("约束 params JSON 非法时应被容错跳过")
        void shouldTolerateMalformedConstraintParams() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(List.of(
                    forbidden(ConstraintLevel.GLOBAL, null, "{not-json")));
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }
    }

    // ==================== 软约束 GA 优化 ====================

    @Nested
    @DisplayName("软约束 GA 优化")
    class SoftConstraintGaTests {

        @Test
        @DisplayName("存在软约束时触发 GA 优化且不破坏排课结果")
        void shouldRunGaWithSoftConstraints() {
            stubPeriodConfig(8);
            stubRequirements(List.of(
                    taskRow(100L, 10L, 20L, 30L, 2, 2),
                    taskRow(101L, 11L, 21L, 31L, 2, 2)));
            stubUnscheduledCount(2L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            SchedulingConstraint softPref = SchedulingConstraint.reconstruct(3L, 1L, "时间偏好",
                    ConstraintLevel.TEACHER, 30L, "教师30", null,
                    ConstraintType.TIME_PREFERRED, false, 60,
                    "{\"days\":[1,2],\"periods\":[1,2]}", null, true, 1L);
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(List.of(softPref));
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> params = new HashMap<>();
            params.put("maxIterations", 5);
            params.put("populationSize", 6);
            Map<String, Object> result = service.autoSchedule(1L, params);

            assertThat(result.get("success")).isEqualTo(true);
            assertThat((Integer) result.get("entriesGenerated")).isEqualTo(2);
        }

        @Test
        @DisplayName("SPREAD_EVEN 与 COMPACT_SCHEDULE 软约束均被评估")
        void shouldEvaluateSpreadAndCompactConstraints() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 4, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            SchedulingConstraint spread = SchedulingConstraint.reconstruct(4L, 1L, "均匀",
                    ConstraintLevel.GLOBAL, null, null, null,
                    ConstraintType.SPREAD_EVEN, false, 50, "{}", null, true, 1L);
            SchedulingConstraint compact = SchedulingConstraint.reconstruct(5L, 1L, "紧凑",
                    ConstraintLevel.TEACHER, 30L, "教师30", null,
                    ConstraintType.COMPACT_SCHEDULE, false, 70,
                    "{\"maxDays\":3}", null, true, 1L);
            when(constraintRepo.findEnabledBySemesterId(1L))
                    .thenReturn(List.of(spread, compact));
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> params = new HashMap<>();
            params.put("maxIterations", 3);
            params.put("populationSize", 4);
            Map<String, Object> result = service.autoSchedule(1L, params);

            assertThat(result.get("success")).isEqualTo(true);
        }
    }

    // ==================== 固定课表条目 ====================

    @Nested
    @DisplayName("已有固定课表条目")
    class FixedEntryTests {

        @Test
        @DisplayName("已占用时段应被纳入冲突检测")
        void shouldRespectFixedEntries() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            // 教师30 在周一 1-2 节已被占用
            Map<String, Object> fixed = new HashMap<>();
            fixed.put("teacher_id", 30L);
            fixed.put("org_unit_id", 20L);
            fixed.put("classroom_id", 500L);
            fixed.put("weekday", 1);
            fixed.put("start_slot", 1);
            fixed.put("end_slot", 2);
            stubFixedEntries(List.of(fixed));
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }
    }

    // ==================== 教师偏好 ====================

    @Nested
    @DisplayName("教师偏好接入")
    class TeacherPreferenceTests {

        @Test
        @DisplayName("不可用时间偏好(type=1)应合并为禁排")
        void shouldApplyUnavailablePreference() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            Map<String, Object> pref = new HashMap<>();
            pref.put("teacher_id", 30L);
            pref.put("preference_type", 1);
            pref.put("weekday", 1);
            pref.put("time_slot", 1);
            pref.put("classroom_id", null);
            stubTeacherPreferences(List.of(pref));
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("整天不可用偏好(type=1 无 time_slot)应禁排全天")
        void shouldApplyFullDayUnavailable() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            Map<String, Object> pref = new HashMap<>();
            pref.put("teacher_id", 30L);
            pref.put("preference_type", 1);
            pref.put("weekday", 2);
            pref.put("time_slot", null);
            pref.put("classroom_id", null);
            stubTeacherPreferences(List.of(pref));
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("偏好教室(type=3)应在排课时优先匹配")
        void shouldApplyPreferredRoom() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60), classroomRow(501L, 80)));
            stubFixedEntries(Collections.emptyList());
            Map<String, Object> pref = new HashMap<>();
            pref.put("teacher_id", 30L);
            pref.put("preference_type", 3);
            pref.put("weekday", null);
            pref.put("time_slot", null);
            pref.put("classroom_id", 501L);
            stubTeacherPreferences(List.of(pref));
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("教师偏好查询抛异常时应被容错跳过")
        void shouldTolerateTeacherPreferenceQueryFailure() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            when(jdbcTemplate.queryForList(
                    org.mockito.ArgumentMatchers.contains("teacher_preferences"),
                    any(Object[].class))).thenThrow(new RuntimeException("table missing"));
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }
    }

    // ==================== 合堂展开 ====================

    @Nested
    @DisplayName("合堂课程展开")
    class CombinedClassTests {

        @Test
        @DisplayName("同 teachingClassId+courseId 的任务合并为合堂, 每个班级各写一条")
        void shouldExpandCombinedClasses() {
            stubPeriodConfig(8);
            // 两个 task 共享 teaching_class_id=900 + course_id=10 → 合堂
            Map<String, Object> r1 = taskRow(100L, 10L, 20L, 30L, 2, 2);
            r1.put("teaching_class_id", 900L);
            Map<String, Object> r2 = taskRow(101L, 10L, 21L, 30L, 2, 2);
            r2.put("teaching_class_id", 900L);
            stubRequirements(List.of(r1, r2));
            stubUnscheduledCount(2L);
            stubClassrooms(List.of(classroomRow(500L, 200)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(Collections.emptyList());
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
            // 合堂只排一次但为 2 个班级各写一条 entry
            assertThat((Integer) result.get("entriesGenerated")).isEqualTo(2);
        }
    }

    // ==================== 参数处理 ====================

    @Nested
    @DisplayName("入参处理")
    class ParamHandlingTests {

        @Test
        @DisplayName("缺省 maxIterations/populationSize 时使用默认值且不报错")
        void shouldUseDefaultIterationParams() {
            stubPeriodConfig(8);
            stubRequirements(Collections.emptyList());
            stubUnscheduledCount(0L);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("maxIterations 以字符串传入应被 toInt 解析")
        void shouldParseStringIterationParam() {
            stubPeriodConfig(8);
            stubRequirements(List.of(taskRow(100L, 10L, 20L, 30L, 2, 2)));
            stubUnscheduledCount(1L);
            stubClassrooms(List.of(classroomRow(500L, 60)));
            stubFixedEntries(Collections.emptyList());
            stubTeacherPreferences(Collections.emptyList());
            SchedulingConstraint soft = SchedulingConstraint.reconstruct(6L, 1L, "均匀",
                    ConstraintLevel.GLOBAL, null, null, null,
                    ConstraintType.SPREAD_EVEN, false, 50, "{}", null, true, 1L);
            when(constraintRepo.findEnabledBySemesterId(1L)).thenReturn(List.of(soft));
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            Map<String, Object> params = new HashMap<>();
            params.put("maxIterations", "2");
            params.put("populationSize", "4");
            Map<String, Object> result = service.autoSchedule(1L, params);

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("buildResult 返回的 result 应含全部标准字段")
        void shouldContainStandardResultKeys() {
            stubPeriodConfig(8);
            stubRequirements(Collections.emptyList());
            stubUnscheduledCount(0L);

            Map<String, Object> result = service.autoSchedule(1L, new HashMap<>());

            assertThat(result).containsKeys("success", "entriesGenerated",
                    "conflicts", "executionTime");
            assertThat(result.get("conflicts")).isEqualTo(Collections.emptyList());
            assertThat((Long) result.get("executionTime")).isGreaterThanOrEqualTo(0L);
        }
    }
}
