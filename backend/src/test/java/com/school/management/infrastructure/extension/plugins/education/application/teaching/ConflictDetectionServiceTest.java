package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.ConstraintLevel;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.ConstraintType;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.ScheduleConflictRecord;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.SchedulingConstraint;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.ScheduleConflictRecordRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.SchedulingConstraintRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ConflictDetectionService 单测 — 验证排课可行性检查与冲突检测逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConflictDetectionService 测试")
class ConflictDetectionServiceTest {

    @Mock
    private ScheduleConflictRecordRepository conflictRepo;

    @Mock
    private SchedulingConstraintRepository constraintRepo;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ConflictDetectionService service;

    private Map<String, Object> teacherTaskRow(long teacherId, String name, int hours) {
        Map<String, Object> m = new HashMap<>();
        m.put("teacher_id", teacherId);
        m.put("teacher_name", name);
        m.put("total_hours", hours);
        return m;
    }

    private Map<String, Object> conflictRow(long id1, long id2, Long orgUnitId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id1", id1);
        m.put("id2", id2);
        m.put("teacher_id", 1L);
        m.put("classroom_id", 1L);
        m.put("org_unit_id", orgUnitId);
        m.put("weekday", 1);
        m.put("start_slot", 1);
        m.put("end_slot", 2);
        return m;
    }

    @Nested
    @DisplayName("feasibilityCheck 可行性检查")
    class FeasibilityCheckTests {

        @Test
        @DisplayName("教师课时在可用槽内时记为通过, 无阻塞问题")
        void shouldPassWhenSlotsSufficient() {
            // 无约束 → 5*10=50 可用槽; 课时 20 ≤ 50
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(List.of(teacherTaskRow(1L, "张老师", 20)))
                    .thenReturn(Collections.emptyList()); // oversized classes
            when(constraintRepo.findEnabledBySemesterId(10L))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> report = service.feasibilityCheck(10L);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> blocking = (List<Map<String, Object>>) report.get("blockingIssues");
            assertThat(blocking).isEmpty();
            // teacher passed + room check passed = 2
            assertThat(report.get("passedChecks")).isEqualTo(2);
        }

        @Test
        @DisplayName("教师课时超过可用槽时生成 TEACHER_TIME_INSUFFICIENT 阻塞问题")
        void shouldBlockWhenTeacherSlotsInsufficient() {
            // 课时 60 > 50 可用槽
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(List.of(teacherTaskRow(1L, "李老师", 60)))
                    .thenReturn(Collections.emptyList());
            when(constraintRepo.findEnabledBySemesterId(10L))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> report = service.feasibilityCheck(10L);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> blocking = (List<Map<String, Object>>) report.get("blockingIssues");
            assertThat(blocking).hasSize(1);
            assertThat(blocking.get(0).get("type")).isEqualTo("TEACHER_TIME_INSUFFICIENT");
            assertThat(blocking.get(0).get("target")).isEqualTo("李老师");
            // 只有 room check 通过
            assertThat(report.get("passedChecks")).isEqualTo(1);
        }

        @Test
        @DisplayName("全局硬性禁排约束应减少可用槽并可能导致课时不足")
        void shouldReduceSlotsWithGlobalForbiddenConstraint() throws Exception {
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(List.of(teacherTaskRow(1L, "王老师", 50)))
                    .thenReturn(Collections.emptyList());
            // 全局约束: 周一全天禁排 (10 个槽) → 可用槽 40 < 50
            SchedulingConstraint global = SchedulingConstraint.create(
                    10L, "周一禁排", ConstraintLevel.GLOBAL, null, null, null,
                    ConstraintType.TIME_FORBIDDEN, true, 50,
                    "{\"days\":[1]}", null, 99L);
            when(constraintRepo.findEnabledBySemesterId(10L)).thenReturn(List.of(global));
            Map<String, Object> params = new HashMap<>();
            params.put("days", List.of(1));
            params.put("periods", Collections.emptyList());
            when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(params);

            Map<String, Object> report = service.feasibilityCheck(10L);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> blocking = (List<Map<String, Object>>) report.get("blockingIssues");
            assertThat(blocking).hasSize(1);
            assertThat(blocking.get(0).get("type")).isEqualTo("TEACHER_TIME_INSUFFICIENT");
        }

        @Test
        @DisplayName("教师级约束仅影响该教师的可用槽")
        void shouldApplyTeacherLevelConstraint() throws Exception {
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(List.of(teacherTaskRow(1L, "赵老师", 50)))
                    .thenReturn(Collections.emptyList());
            SchedulingConstraint teacherConstraint = SchedulingConstraint.create(
                    10L, "赵老师周二禁排", ConstraintLevel.TEACHER, 1L, "赵老师", null,
                    ConstraintType.TIME_FORBIDDEN, true, 50,
                    "{\"days\":[2]}", null, 99L);
            when(constraintRepo.findEnabledBySemesterId(10L))
                    .thenReturn(List.of(teacherConstraint));
            Map<String, Object> params = new HashMap<>();
            params.put("days", List.of(2));
            params.put("periods", Collections.emptyList());
            when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(params);

            Map<String, Object> report = service.feasibilityCheck(10L);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> blocking = (List<Map<String, Object>>) report.get("blockingIssues");
            // 周二 10 槽禁排 → 40 可用 < 50
            assertThat(blocking).hasSize(1);
        }

        @Test
        @DisplayName("超容量教学班应生成 ROOM_CAPACITY_INSUFFICIENT 阻塞问题")
        void shouldBlockWhenClassOversized() {
            Map<String, Object> oversized = new HashMap<>();
            oversized.put("class_name", "计科2401");
            oversized.put("student_count", 200);
            oversized.put("required_capacity", 200);
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(Collections.emptyList()) // no teacher tasks
                    .thenReturn(List.of(oversized));
            when(constraintRepo.findEnabledBySemesterId(10L))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> report = service.feasibilityCheck(10L);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> blocking = (List<Map<String, Object>>) report.get("blockingIssues");
            assertThat(blocking).hasSize(1);
            assertThat(blocking.get(0).get("type")).isEqualTo("ROOM_CAPACITY_INSUFFICIENT");
            assertThat(blocking.get(0).get("target")).isEqualTo("计科2401");
        }

        @Test
        @DisplayName("软约束(isHard=false)不应减少可用槽")
        void shouldIgnoreSoftConstraint() {
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(List.of(teacherTaskRow(1L, "孙老师", 50)))
                    .thenReturn(Collections.emptyList());
            SchedulingConstraint soft = SchedulingConstraint.create(
                    10L, "软约束", ConstraintLevel.GLOBAL, null, null, null,
                    ConstraintType.TIME_FORBIDDEN, false, 50,
                    "{\"days\":[1]}", null, 99L);
            when(constraintRepo.findEnabledBySemesterId(10L)).thenReturn(List.of(soft));
            // isHard=false → isTimeForbidden 直接 false, 不调 objectMapper

            Map<String, Object> report = service.feasibilityCheck(10L);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> blocking = (List<Map<String, Object>>) report.get("blockingIssues");
            assertThat(blocking).isEmpty();
        }
    }

    @Nested
    @DisplayName("detectConflicts 排课冲突检测")
    class DetectConflictsTests {

        @Test
        @DisplayName("无冲突时返回空列表且不保存")
        void shouldReturnEmptyWhenNoConflicts() {
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(Collections.emptyList())
                    .thenReturn(Collections.emptyList())
                    .thenReturn(Collections.emptyList());

            List<ScheduleConflictRecord> result = service.detectConflicts(10L, 99L);

            assertThat(result).isEmpty();
            verify(conflictRepo, never()).save(any(ScheduleConflictRecord.class));
        }

        @Test
        @DisplayName("检测到教师冲突应保存 TEACHER_CONFLICT 记录")
        void shouldDetectTeacherConflict() {
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(List.of(conflictRow(1L, 2L, 800L))) // teacher
                    .thenReturn(Collections.emptyList())             // room
                    .thenReturn(Collections.emptyList());            // class
            when(conflictRepo.save(any(ScheduleConflictRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<ScheduleConflictRecord> result = service.detectConflicts(10L, 99L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getConflictType()).isEqualTo("TEACHER_CONFLICT");
            assertThat(result.get(0).getEntryId1()).isEqualTo(1L);
            assertThat(result.get(0).getEntryId2()).isEqualTo(2L);
            assertThat(result.get(0).getOrgUnitId()).isEqualTo(800L);
        }

        @Test
        @DisplayName("检测到教室冲突应保存 CLASSROOM_CONFLICT 记录")
        void shouldDetectRoomConflict() {
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(Collections.emptyList())
                    .thenReturn(List.of(conflictRow(3L, 4L, 801L)))
                    .thenReturn(Collections.emptyList());
            when(conflictRepo.save(any(ScheduleConflictRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<ScheduleConflictRecord> result = service.detectConflicts(10L, 99L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getConflictType()).isEqualTo("CLASSROOM_CONFLICT");
        }

        @Test
        @DisplayName("检测到班级冲突应保存 CLASS_CONFLICT 记录")
        void shouldDetectClassConflict() {
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(Collections.emptyList())
                    .thenReturn(Collections.emptyList())
                    .thenReturn(List.of(conflictRow(5L, 6L, 802L)));
            when(conflictRepo.save(any(ScheduleConflictRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<ScheduleConflictRecord> result = service.detectConflicts(10L, 99L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getConflictType()).isEqualTo("CLASS_CONFLICT");
        }

        @Test
        @DisplayName("三类冲突同时存在时应全部检出")
        void shouldDetectAllConflictTypes() {
            when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                    .thenReturn(List.of(conflictRow(1L, 2L, 800L)))
                    .thenReturn(List.of(conflictRow(3L, 4L, 801L)))
                    .thenReturn(List.of(conflictRow(5L, 6L, null)));
            when(conflictRepo.save(any(ScheduleConflictRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<ScheduleConflictRecord> result = service.detectConflicts(10L, 99L);

            assertThat(result).hasSize(3);
            verify(conflictRepo, times(3)).save(any(ScheduleConflictRecord.class));
        }
    }

    @Nested
    @DisplayName("listConflicts 冲突查询")
    class ListConflictsTests {

        @Test
        @DisplayName("status 非空时按状态过滤")
        void shouldFilterByStatus() {
            when(conflictRepo.findBySemesterIdAndStatus(10L, 0))
                    .thenReturn(Collections.emptyList());

            service.listConflicts(10L, 0);

            verify(conflictRepo).findBySemesterIdAndStatus(10L, 0);
            verify(conflictRepo, never()).findBySemesterId(any());
        }

        @Test
        @DisplayName("status 为空时仅按学期查询")
        void shouldQueryBySemesterWhenStatusNull() {
            when(conflictRepo.findBySemesterId(10L))
                    .thenReturn(Collections.emptyList());

            service.listConflicts(10L, null);

            verify(conflictRepo).findBySemesterId(10L);
        }
    }

    @Nested
    @DisplayName("resolveConflict / ignoreConflict 冲突处理")
    class ResolveIgnoreTests {

        private ScheduleConflictRecord sampleRecord() {
            return ScheduleConflictRecord.create(
                    10L, "DET-1", 1, "TEACHER_CONFLICT", 1,
                    "desc", null, 1L, 2L, null, 800L, 99L);
        }

        @Test
        @DisplayName("resolveConflict 应将状态置为已解决并 save")
        void shouldResolve() {
            ScheduleConflictRecord r = sampleRecord();
            when(conflictRepo.findById(1L)).thenReturn(Optional.of(r));

            service.resolveConflict(1L, "已调课", 99L);

            assertThat(r.getResolutionStatus()).isEqualTo(1);
            assertThat(r.getResolutionNote()).isEqualTo("已调课");
            assertThat(r.getResolvedBy()).isEqualTo(99L);
            verify(conflictRepo).save(r);
        }

        @Test
        @DisplayName("resolveConflict 不存在时抛出 IllegalArgumentException")
        void shouldThrowWhenResolveNotFound() {
            when(conflictRepo.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolveConflict(1L, "note", 99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("冲突记录不存在");
        }

        @Test
        @DisplayName("ignoreConflict 应将状态置为已忽略并 save")
        void shouldIgnore() {
            ScheduleConflictRecord r = sampleRecord();
            when(conflictRepo.findById(2L)).thenReturn(Optional.of(r));

            service.ignoreConflict(2L, "可接受", 99L);

            assertThat(r.getResolutionStatus()).isEqualTo(2);
            assertThat(r.getResolutionNote()).isEqualTo("可接受");
            verify(conflictRepo).save(r);
        }

        @Test
        @DisplayName("ignoreConflict 不存在时抛出 IllegalArgumentException")
        void shouldThrowWhenIgnoreNotFound() {
            when(conflictRepo.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.ignoreConflict(2L, "note", 99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
