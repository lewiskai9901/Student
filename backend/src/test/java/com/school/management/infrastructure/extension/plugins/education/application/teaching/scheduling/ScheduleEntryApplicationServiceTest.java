package com.school.management.infrastructure.extension.plugins.education.application.teaching.scheduling;

import com.school.management.application.event.TriggerService;
import com.school.management.infrastructure.extension.plugins.education.application.teaching.AutoSchedulingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ScheduleEntryApplicationService 单测 — 验证排课配置 / 方案 / 课表条目 /
 * 锁定 / 重置 / 自习课 / 移动冲突 等 JDBC 行为与 SQL 组装.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleEntryApplicationService 测试")
class ScheduleEntryApplicationServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private AutoSchedulingService autoSchedulingService;

    @Mock
    private TriggerService triggerService;

    private ScheduleEntryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleEntryApplicationService(jdbc);
        // 模拟登录用户 (requireCurrentUserId 读 SecurityContextHolder)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("99", null,
                        Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void injectAutoScheduling() {
        ReflectionTestUtils.setField(service, "autoSchedulingService", autoSchedulingService);
    }

    private void injectTrigger() {
        ReflectionTestUtils.setField(service, "triggerService", triggerService);
    }

    // ==================== 节次配置 ====================

    @Nested
    @DisplayName("节次配置")
    class ScheduleConfigTests {

        @Test
        @DisplayName("getScheduleConfig 应解析存储的 JSON 配置")
        void shouldReadStoredConfig() {
            when(jdbc.queryForObject(anyString(), eq(String.class), eq("schedule.periods.1")))
                    .thenReturn("{\"periodsPerDay\":10}");

            Map<String, Object> config = service.getScheduleConfig(1L);

            assertThat(config.get("periodsPerDay")).isEqualTo(10);
        }

        @Test
        @DisplayName("无记录(EmptyResultDataAccessException) 时返回默认配置")
        void shouldReturnDefaultWhenNoRecord() {
            when(jdbc.queryForObject(anyString(), eq(String.class), anyString()))
                    .thenThrow(new EmptyResultDataAccessException(1));

            Map<String, Object> config = service.getScheduleConfig(1L);

            assertThat(config.get("periodsPerDay")).isEqualTo(8);
            assertThat(config).containsKey("scheduleDays");
            assertThat(config).containsKey("periods");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> periods =
                    (List<Map<String, Object>>) config.get("periods");
            assertThat(periods).hasSize(8);
        }

        @Test
        @DisplayName("JSON 解析失败时返回兜底 {periodsPerDay:8}")
        void shouldReturnFallbackWhenJsonInvalid() {
            when(jdbc.queryForObject(anyString(), eq(String.class), anyString()))
                    .thenReturn("{not-json");

            Map<String, Object> config = service.getScheduleConfig(1L);

            assertThat(config.get("periodsPerDay")).isEqualTo(8);
        }

        @Test
        @DisplayName("saveScheduleConfig 已存在时执行 UPDATE")
        void shouldUpdateExistingConfig() {
            when(jdbc.queryForObject(anyString(), eq(Long.class), anyString()))
                    .thenReturn(1L);

            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 1L);
            data.put("periodsPerDay", 9);
            boolean ok = service.saveScheduleConfig(data);

            assertThat(ok).isTrue();
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(), any(), any());
            assertThat(sqlCap.getValue()).contains("UPDATE system_configs");
        }

        @Test
        @DisplayName("saveScheduleConfig 不存在时执行 INSERT")
        void shouldInsertNewConfig() {
            when(jdbc.queryForObject(anyString(), eq(Long.class), anyString()))
                    .thenReturn(0L);

            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 2L);
            data.put("periodsPerDay", 8);
            boolean ok = service.saveScheduleConfig(data);

            assertThat(ok).isTrue();
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(), any(), any());
            assertThat(sqlCap.getValue()).contains("INSERT INTO system_configs");
        }

        @Test
        @DisplayName("saveScheduleConfig 抛异常时返回 false")
        void shouldReturnFalseWhenSaveFails() {
            when(jdbc.queryForObject(anyString(), eq(Long.class), anyString()))
                    .thenThrow(new RuntimeException("db down"));

            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 1L);
            boolean ok = service.saveScheduleConfig(data);

            assertThat(ok).isFalse();
        }
    }

    // ==================== 数据就绪检查 ====================

    @Nested
    @DisplayName("数据就绪检查")
    class ReadinessTests {

        @Test
        @DisplayName("checkReadiness 全部有数据时 status=ready")
        void shouldReportReadyWhenAllPresent() {
            when(jdbc.queryForObject(anyString(), eq(Long.class), any()))
                    .thenReturn(5L);
            // 教室数量查询无 ? 参数, 走 queryForObject(String, Class) 两参重载
            when(jdbc.queryForObject(anyString(), eq(Long.class)))
                    .thenReturn(5L);

            Map<String, Object> result = service.checkReadiness(1L);

            assertThat(result).containsKeys("offerings", "tasks", "classrooms",
                    "constraints", "plans");
            @SuppressWarnings("unchecked")
            Map<String, Object> offerings = (Map<String, Object>) result.get("offerings");
            assertThat(offerings.get("status")).isEqualTo("ready");
        }

        @Test
        @DisplayName("checkReadiness 全为 0 时 status=empty")
        void shouldReportEmptyWhenNoData() {
            when(jdbc.queryForObject(anyString(), eq(Long.class), any()))
                    .thenReturn(0L);
            // 教室数量查询无 ? 参数, 走 queryForObject(String, Class) 两参重载
            when(jdbc.queryForObject(anyString(), eq(Long.class)))
                    .thenReturn(0L);

            Map<String, Object> result = service.checkReadiness(1L);

            @SuppressWarnings("unchecked")
            Map<String, Object> tasks = (Map<String, Object>) result.get("tasks");
            assertThat(tasks.get("status")).isEqualTo("empty");
        }
    }

    // ==================== 排课方案 ====================

    @Nested
    @DisplayName("排课方案 CRUD")
    class SchedulePlanTests {

        @Test
        @DisplayName("listSchedulePlans 同时带 semesterId+status 时 SQL 含两个过滤")
        void shouldListPlansWithFilters() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.listSchedulePlans(1L, 0);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("semester_id = ?");
            assertThat(sql).contains("status = ?");
        }

        @Test
        @DisplayName("listSchedulePlans 无过滤参数时 SQL 不含可选条件")
        void shouldListPlansWithoutFilters() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.listSchedulePlans(null, null);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).doesNotContain("semester_id = ?");
        }

        @Test
        @DisplayName("getSchedulePlan 应按 id 查询单条")
        void shouldGetPlan() {
            when(jdbc.queryForMap(anyString(), eq(7L)))
                    .thenReturn(Map.of("id", 7L));

            Map<String, Object> plan = service.getSchedulePlan(7L);

            assertThat(plan.get("id")).isEqualTo(7L);
        }

        @Test
        @DisplayName("createSchedulePlan 应 INSERT 并回查 LAST_INSERT_ID")
        void shouldCreatePlan() {
            when(jdbc.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class)))
                    .thenReturn(123L);

            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 1L);
            data.put("name", "方案A");
            Map<String, Object> result = service.createSchedulePlan(data);

            assertThat(result.get("id")).isEqualTo(123L);
            assertThat(result.get("name")).isEqualTo("方案A");
            assertThat(result.get("status")).isEqualTo(0);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(), any(), any(), any());
            assertThat(sqlCap.getValue()).contains("INSERT INTO course_schedules");
        }

        @Test
        @DisplayName("updateSchedulePlan 应执行 UPDATE")
        void shouldUpdatePlan() {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "新名");
            service.updateSchedulePlan(5L, data);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq("新名"), eq(""), eq(99L), eq(5L));
            assertThat(sqlCap.getValue()).contains("UPDATE course_schedules SET name");
        }

        @Test
        @DisplayName("deleteSchedulePlan 应软删方案并级联软删条目")
        void shouldDeletePlanAndEntries() {
            service.deleteSchedulePlan(8L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc, times(2)).update(sqlCap.capture(), eq(8L));
            assertThat(sqlCap.getAllValues().get(0)).contains("UPDATE course_schedules SET deleted = 1");
            assertThat(sqlCap.getAllValues().get(1)).contains("UPDATE schedule_entries SET deleted = 1");
        }

        @Test
        @DisplayName("publishSchedulePlan 应置 status=1 并写 published_at")
        void shouldPublishPlan() {
            service.publishSchedulePlan(9L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(99L), eq(9L));
            assertThat(sqlCap.getValue()).contains("status = 1");
            assertThat(sqlCap.getValue()).contains("published_at = NOW()");
        }

        @Test
        @DisplayName("archiveSchedulePlan 应置 status=2")
        void shouldArchivePlan() {
            service.archiveSchedulePlan(10L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(99L), eq(10L));
            assertThat(sqlCap.getValue()).contains("status = 2");
        }
    }

    // ==================== 课表条目 ====================

    @Nested
    @DisplayName("课表条目 CRUD")
    class ScheduleEntryTests {

        @Test
        @DisplayName("listSchedules 带 semesterId+status 时含两过滤并 ORDER BY")
        void shouldListSchedules() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.listSchedules(1L, 1);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("semester_id = ?");
            assertThat(sql).contains("entry_status = ?");
            assertThat(sql).contains("ORDER BY weekday, start_slot");
        }

        @Test
        @DisplayName("getSchedule 应按 id 查询单条")
        void shouldGetSchedule() {
            when(jdbc.queryForMap(anyString(), eq(3L))).thenReturn(Map.of("id", 3L));

            assertThat(service.getSchedule(3L).get("id")).isEqualTo(3L);
        }

        @Test
        @DisplayName("createSchedule 应 INSERT 并在结果中带回 id")
        void shouldCreateSchedule() {
            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 1);
            data.put("taskId", 2);
            data.put("courseId", 3);
            data.put("orgUnitId", 4);
            data.put("teacherId", 5);
            data.put("classroomId", 6);
            data.put("weekday", 1);
            data.put("startSlot", 1);
            data.put("endSlot", 2);

            Map<String, Object> result = service.createSchedule(data);

            assertThat(result.get("id")).isInstanceOf(Long.class);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).contains("INSERT INTO schedule_entries");
        }

        @Test
        @DisplayName("createSchedule endWeek 缺省时调用 getSemesterTeachingWeeks 取学期周数")
        void shouldUseSemesterWeeksWhenEndWeekMissing() {
            when(jdbc.queryForObject(
                    org.mockito.ArgumentMatchers.contains("academic_weeks"),
                    eq(Integer.class), any())).thenReturn(20);

            Map<String, Object> data = new HashMap<>();
            data.put("semesterId", 1);
            data.put("weekday", 1);
            data.put("startSlot", 1);
            data.put("endSlot", 2);

            service.createSchedule(data);

            verify(jdbc).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("updateSchedule 仅更新提供的字段")
        void shouldUpdatePartialFields() {
            Map<String, Object> data = new HashMap<>();
            data.put("weekday", 3);
            data.put("startSlot", 5);

            service.updateSchedule(7L, data);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("weekday = ?");
            assertThat(sql).contains("start_slot = ?");
            assertThat(sql).doesNotContain("end_slot = ?");
        }

        @Test
        @DisplayName("updateSchedule 无任何字段时直接返回不执行 SQL")
        void shouldNoopWhenNoFields() {
            service.updateSchedule(7L, new HashMap<>());

            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("updateSchedule 支持 teacherId/classroomId 为 null")
        void shouldUpdateNullableForeignKeys() {
            Map<String, Object> data = new HashMap<>();
            data.put("teacherId", null);
            data.put("classroomId", null);
            data.put("endWeek", 18);
            data.put("weekType", 1);

            service.updateSchedule(7L, data);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("teacher_id = ?");
            assertThat(sql).contains("classroom_id = ?");
            assertThat(sql).contains("end_week = ?");
            assertThat(sql).contains("week_type = ?");
        }

        @Test
        @DisplayName("deleteSchedule 应软删条目")
        void shouldDeleteSchedule() {
            service.deleteSchedule(11L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(11L));
            assertThat(sqlCap.getValue()).contains("UPDATE schedule_entries SET deleted = 1");
        }
    }

    // ==================== 锁定 ====================

    @Nested
    @DisplayName("锁定/解锁")
    class LockTests {

        @Test
        @DisplayName("toggleLock 应翻转 is_locked 并回查结果")
        void shouldToggleLock() {
            when(jdbc.queryForObject(
                    org.mockito.ArgumentMatchers.contains("SELECT is_locked"),
                    eq(Integer.class), eq(5L))).thenReturn(1);

            Map<String, Object> result = service.toggleLock(5L);

            assertThat(result.get("isLocked")).isEqualTo(1);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(5L));
            assertThat(sqlCap.getValue()).contains("is_locked = 1 - is_locked");
        }

        @Test
        @DisplayName("toggleLock 回查为 null 时 isLocked 默认 0")
        void shouldDefaultZeroWhenLockNull() {
            when(jdbc.queryForObject(
                    org.mockito.ArgumentMatchers.contains("SELECT is_locked"),
                    eq(Integer.class), eq(5L))).thenReturn(null);

            Map<String, Object> result = service.toggleLock(5L);

            assertThat(result.get("isLocked")).isEqualTo(0);
        }

        @Test
        @DisplayName("batchLock 空 ids 列表时返回 updated=0 不执行 SQL")
        void shouldReturnZeroForEmptyBatch() {
            Map<String, Object> result = service.batchLock(Collections.emptyList(), 1);

            assertThat(result.get("updated")).isEqualTo(0);
            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("batchLock null ids 时返回 updated=0")
        void shouldReturnZeroForNullBatch() {
            Map<String, Object> result = service.batchLock(null, 1);

            assertThat(result.get("updated")).isEqualTo(0);
        }

        @Test
        @DisplayName("batchLock 应构造 IN 占位符并传 lock 值")
        void shouldBatchLockWithPlaceholders() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(3);

            List<Number> ids = List.of(1L, 2L, 3L);
            Map<String, Object> result = service.batchLock(ids, 1);

            assertThat(result.get("updated")).isEqualTo(3);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> argsCap = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).update(sqlCap.capture(), argsCap.capture());
            assertThat(sqlCap.getValue()).contains("id IN (?,?,?)");
            // 首参为 lock 值
            assertThat(argsCap.getValue()[0]).isEqualTo(1);
            assertThat(argsCap.getValue()).hasSize(4);
        }
    }

    // ==================== 重置排课 ====================

    @Nested
    @DisplayName("重置排课")
    class ResetScheduleTests {

        @Test
        @DisplayName("resetSchedule keepLocked=false 时清全部条目")
        void shouldResetAllEntries() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(4);

            Map<String, Object> result = service.resetSchedule(1L, false);

            assertThat(result.get("keepLocked")).isEqualTo(false);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc, atLeastOnce()).update(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getAllValues().get(0))
                    .contains("UPDATE schedule_entries SET deleted = 1")
                    .doesNotContain("is_locked = 0");
        }

        @Test
        @DisplayName("resetSchedule keepLocked=true 时保留锁定条目")
        void shouldResetKeepingLocked() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(2);

            Map<String, Object> result = service.resetSchedule(1L, true);

            assertThat(result.get("keepLocked")).isEqualTo(true);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc, atLeastOnce()).update(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getAllValues().get(0)).contains("is_locked = 0");
        }
    }

    // ==================== 教师 / 课表查询 ====================

    @Nested
    @DisplayName("课表查询")
    class ScheduleQueryTests {

        @Test
        @DisplayName("getTeachersGroupedByDept 应按部门排序查教师")
        void shouldGetTeachersGroupedByDept() {
            when(jdbc.queryForList(anyString())).thenReturn(Collections.emptyList());

            service.getTeachersGroupedByDept();

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture());
            assertThat(sqlCap.getValue()).contains("user_type_code = 'TEACHER'");
        }

        @Test
        @DisplayName("getSchedulesByClass 带 semesterId 时含 join 与学期过滤")
        void shouldGetSchedulesByClass() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.getSchedulesByClass(20L, 1L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("se.org_unit_id = ?");
            assertThat(sql).contains("se.semester_id = ?");
            assertThat(sql).contains("LEFT JOIN courses c");
        }

        @Test
        @DisplayName("getSchedulesByClass 无 semesterId 时不含学期过滤")
        void shouldGetSchedulesByClassWithoutSemester() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.getSchedulesByClass(20L, null);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).doesNotContain("se.semester_id = ?");
        }

        @Test
        @DisplayName("getSchedulesByTeacher 应按 teacher_id 过滤")
        void shouldGetSchedulesByTeacher() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.getSchedulesByTeacher(30L, 1L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).contains("se.teacher_id = ?");
        }

        @Test
        @DisplayName("getSchedulesByClassroom 应按 classroom_id 过滤")
        void shouldGetSchedulesByClassroom() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.getSchedulesByClassroom(500L, null);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).contains("se.classroom_id = ?");
        }
    }

    // ==================== 自动排课 ====================

    @Nested
    @DisplayName("自动排课入口")
    class AutoScheduleTests {

        @Test
        @DisplayName("autoSchedulingService 未注入时抛 IllegalStateException")
        void shouldThrowWhenAutoSchedulingMissing() {
            Map<String, Object> params = new HashMap<>();
            params.put("semesterId", 1L);

            assertThatThrownBy(() -> service.autoSchedule(params))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("排课服务未启用");
        }

        @Test
        @DisplayName("autoSchedule 应委派 AutoSchedulingService 并返回其结果")
        void shouldDelegateToAutoSchedulingService() {
            injectAutoScheduling();
            Map<String, Object> svcResult = new HashMap<>();
            svcResult.put("success", true);
            svcResult.put("entriesGenerated", 5);
            when(autoSchedulingService.autoSchedule(eq(1L), any()))
                    .thenReturn(svcResult);

            Map<String, Object> params = new HashMap<>();
            params.put("semesterId", 1L);
            Map<String, Object> result = service.autoSchedule(params);

            assertThat(result.get("success")).isEqualTo(true);
            assertThat(result.get("entriesGenerated")).isEqualTo(5);
        }

        @Test
        @DisplayName("triggerService 存在时 autoSchedule 后触发 SCHEDULE_PUBLISHED 事件")
        void shouldFireTriggerAfterSchedule() {
            injectAutoScheduling();
            injectTrigger();
            when(autoSchedulingService.autoSchedule(eq(1L), any()))
                    .thenReturn(new HashMap<>());

            Map<String, Object> params = new HashMap<>();
            params.put("semesterId", 1L);
            service.autoSchedule(params);

            verify(triggerService).fire(eq("SCHEDULE_PUBLISHED"), any());
        }

        @Test
        @DisplayName("triggerService.fire 抛异常时被吞掉, autoSchedule 仍返回结果")
        void shouldSwallowTriggerException() {
            injectAutoScheduling();
            injectTrigger();
            Map<String, Object> svcResult = new HashMap<>();
            svcResult.put("success", true);
            when(autoSchedulingService.autoSchedule(eq(1L), any()))
                    .thenReturn(svcResult);
            org.mockito.Mockito.doThrow(new RuntimeException("trigger fail"))
                    .when(triggerService).fire(anyString(), any());

            Map<String, Object> params = new HashMap<>();
            params.put("semesterId", 1L);
            Map<String, Object> result = service.autoSchedule(params);

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("triggerService 未注入时不触发事件但 autoSchedule 正常返回")
        void shouldSkipTriggerWhenNotInjected() {
            injectAutoScheduling();
            when(autoSchedulingService.autoSchedule(eq(1L), any()))
                    .thenReturn(new HashMap<>());

            Map<String, Object> params = new HashMap<>();
            params.put("semesterId", 1L);
            Map<String, Object> result = service.autoSchedule(params);

            assertThat(result).isNotNull();
            verifyNoInteractions(triggerService);
        }
    }

    // ==================== 拖拽移动 ====================

    @Nested
    @DisplayName("拖拽移动课位")
    class MoveEntryTests {

        @Test
        @DisplayName("moveEntry 应按 span 重算 end_slot 并 UPDATE")
        void shouldMoveEntry() {
            Map<String, Object> entry = new HashMap<>();
            entry.put("span", 1);
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(entry);

            service.moveEntry(5L, 3, 4, null);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(3), eq(4), eq(5), eq(5L));
            assertThat(sqlCap.getValue()).contains("UPDATE schedule_entries SET weekday");
        }

        @Test
        @DisplayName("moveEntry 带 classroomId 时额外执行教室 UPDATE")
        void shouldMoveEntryWithClassroom() {
            Map<String, Object> entry = new HashMap<>();
            entry.put("span", 1);
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(entry);

            service.moveEntry(5L, 3, 4, 500L);

            verify(jdbc).update(
                    org.mockito.ArgumentMatchers.contains("SET classroom_id"),
                    eq(500L), eq(5L));
        }
    }

    // ==================== 移动冲突检测 ====================

    @Nested
    @DisplayName("移动冲突检测")
    class MoveConflictTests {

        @Test
        @DisplayName("无冲突时 hasConflict=false")
        void shouldReportNoConflict() {
            Map<String, Object> entry = new HashMap<>();
            entry.put("teacher_id", 30L);
            entry.put("org_unit_id", 20L);
            entry.put("span", 2);
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(entry);
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> result = service.checkMoveConflict(5L, 1L, 3, 4);

            assertThat(result.get("hasConflict")).isEqualTo(false);
            assertThat(result).containsKeys("teacherConflicts", "classConflicts");
        }

        @Test
        @DisplayName("教师维度有冲突时 hasConflict=true")
        void shouldDetectTeacherConflict() {
            Map<String, Object> entry = new HashMap<>();
            entry.put("teacher_id", 30L);
            entry.put("org_unit_id", null);
            entry.put("span", 2);
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(entry);
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of(Map.of("id", 999L)));

            Map<String, Object> result = service.checkMoveConflict(5L, 1L, 3, 4);

            assertThat(result.get("hasConflict")).isEqualTo(true);
        }

        @Test
        @DisplayName("条目无教师无班级时不查冲突, hasConflict=false")
        void shouldSkipWhenNoTeacherNoClass() {
            Map<String, Object> entry = new HashMap<>();
            entry.put("teacher_id", null);
            entry.put("org_unit_id", null);
            entry.put("span", 1);
            when(jdbc.queryForMap(anyString(), eq(5L))).thenReturn(entry);

            Map<String, Object> result = service.checkMoveConflict(5L, 1L, 3, 4);

            assertThat(result.get("hasConflict")).isEqualTo(false);
            verify(jdbc, never()).queryForList(anyString(), any(Object[].class));
        }
    }

    // ==================== 自习课 ====================

    @Nested
    @DisplayName("自习课填充")
    class SelfStudyTests {

        @Test
        @DisplayName("fillSelfStudy 应为空课位插入 schedule_type=4 自习条目")
        void shouldFillSelfStudy() {
            // 一个班级
            when(jdbc.queryForList(
                    org.mockito.ArgumentMatchers.contains("FROM org_units")))
                    .thenReturn(List.of(Map.of("id", 20L)));
            // 该班级当前无任何占用 → 整周都是空课位
            when(jdbc.queryForList(
                    org.mockito.ArgumentMatchers.contains("FROM schedule_entries"),
                    any(Object[].class))).thenReturn(Collections.emptyList());

            Map<String, Object> result = service.fillSelfStudy(1L, 8, 5, 1, 16);

            assertThat(result.get("classCount")).isEqualTo(1);
            // 5 天 各一段连续空课 → 5 条
            assertThat(result.get("inserted")).isEqualTo(5);
            verify(jdbc, times(5)).update(
                    org.mockito.ArgumentMatchers.contains("INSERT INTO schedule_entries"),
                    any(Object[].class));
        }

        @Test
        @DisplayName("fillSelfStudy 占满课位时不插入任何自习条目")
        void shouldInsertNothingWhenFullyOccupied() {
            when(jdbc.queryForList(
                    org.mockito.ArgumentMatchers.contains("FROM org_units")))
                    .thenReturn(List.of(Map.of("id", 20L)));
            // 周一到周五 1-8 节全部占用
            List<Map<String, Object>> occupied = new ArrayList<>();
            for (int wd = 1; wd <= 5; wd++) {
                Map<String, Object> row = new HashMap<>();
                row.put("weekday", wd);
                row.put("start_slot", 1);
                row.put("end_slot", 8);
                occupied.add(row);
            }
            when(jdbc.queryForList(
                    org.mockito.ArgumentMatchers.contains("FROM schedule_entries"),
                    any(Object[].class))).thenReturn(occupied);

            Map<String, Object> result = service.fillSelfStudy(1L, 8, 5, 1, 16);

            assertThat(result.get("inserted")).isEqualTo(0);
            verify(jdbc, never()).update(
                    org.mockito.ArgumentMatchers.contains("INSERT INTO schedule_entries"),
                    any(Object[].class));
        }

        @Test
        @DisplayName("fillSelfStudy 部分占用时仅填充剩余空段")
        void shouldFillOnlyEmptyGaps() {
            when(jdbc.queryForList(
                    org.mockito.ArgumentMatchers.contains("FROM org_units")))
                    .thenReturn(List.of(Map.of("id", 20L)));
            // 周一 3-4 节占用 → 周一切成 1-2 与 5-8 两段, 其余 4 天各一整段
            Map<String, Object> row = new HashMap<>();
            row.put("weekday", 1);
            row.put("start_slot", 3);
            row.put("end_slot", 4);
            when(jdbc.queryForList(
                    org.mockito.ArgumentMatchers.contains("FROM schedule_entries"),
                    any(Object[].class))).thenReturn(List.of(row));

            Map<String, Object> result = service.fillSelfStudy(1L, 8, 5, 1, 16);

            // 周一 2 段 + 周二~周五 4 段 = 6
            assertThat(result.get("inserted")).isEqualTo(6);
        }

        @Test
        @DisplayName("无班级时 classCount=0 且 inserted=0")
        void shouldHandleNoClasses() {
            when(jdbc.queryForList(
                    org.mockito.ArgumentMatchers.contains("FROM org_units")))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> result = service.fillSelfStudy(1L, 8, 5, 1, 16);

            assertThat(result.get("classCount")).isEqualTo(0);
            assertThat(result.get("inserted")).isEqualTo(0);
        }

        @Test
        @DisplayName("clearSelfStudy 应软删 schedule_type=4 的条目")
        void shouldClearSelfStudy() {
            when(jdbc.update(anyString(), eq(1L))).thenReturn(7);

            Map<String, Object> result = service.clearSelfStudy(1L);

            assertThat(result.get("cleared")).isEqualTo(7);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(1L));
            assertThat(sqlCap.getValue()).contains("schedule_type = 4");
        }
    }

    // ==================== 学期教学周 ====================

    @Nested
    @DisplayName("学期教学周数")
    class SemesterWeeksTests {

        @Test
        @DisplayName("getSemesterTeachingWeeks 应返回 academic_weeks 的最大周号")
        void shouldReturnMaxWeek() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L)))
                    .thenReturn(18);

            assertThat(service.getSemesterTeachingWeeks(1L)).isEqualTo(18);
        }

        @Test
        @DisplayName("查询为 null 时 fallback 16")
        void shouldFallbackWhenNull() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L)))
                    .thenReturn(null);

            assertThat(service.getSemesterTeachingWeeks(1L)).isEqualTo(16);
        }

        @Test
        @DisplayName("查询为 0 时 fallback 16")
        void shouldFallbackWhenZero() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L)))
                    .thenReturn(0);

            assertThat(service.getSemesterTeachingWeeks(1L)).isEqualTo(16);
        }

        @Test
        @DisplayName("查询抛异常时 fallback 16")
        void shouldFallbackWhenQueryThrows() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L)))
                    .thenThrow(new RuntimeException("table missing"));

            assertThat(service.getSemesterTeachingWeeks(1L)).isEqualTo(16);
        }
    }
}
