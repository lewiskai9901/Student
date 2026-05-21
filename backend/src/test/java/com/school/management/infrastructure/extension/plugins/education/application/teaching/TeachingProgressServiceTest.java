package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.exception.TeachingDomainException;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.progress.TeachingProgressMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.progress.TeachingProgressPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TeachingProgressService 单测 — 教学进度 CRUD + 统计 + enrich 批量查询
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeachingProgressService 测试")
class TeachingProgressServiceTest {

    @Mock
    private TeachingProgressMapper mapper;

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private TeachingProgressService service;

    private TeachingProgressPO row(Long id, Long taskId, Long orgUnitId, Long recordedBy) {
        TeachingProgressPO p = new TeachingProgressPO();
        p.setId(id);
        p.setTaskId(taskId);
        p.setOrgUnitId(orgUnitId);
        p.setRecordedBy(recordedBy);
        p.setSemesterId(1L);
        p.setWeekNumber(1);
        p.setLessonNo(1);
        return p;
    }

    /** enrich 默认放行: 所有 jdbc 查询返回空, 不污染断言 */
    private void stubEnrichEmpty() {
        lenient().when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(new ArrayList<>());
        lenient().doNothing().when(jdbc).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
    }

    @Nested
    @DisplayName("listByTask 按任务查询")
    class ListByTaskTests {

        @Test
        @DisplayName("应按 taskId 查询并 enrich 后返回")
        void shouldListByTask() {
            List<TeachingProgressPO> rows = List.of(row(1L, 10L, 9L, 99L));
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rows);
            stubEnrichEmpty();

            List<TeachingProgressPO> result = service.listByTask(10L);

            assertThat(result).hasSize(1);
            verify(mapper).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("无数据 — enrich 提前返回, 不触发 jdbc 查询")
        void shouldSkipEnrichWhenEmpty() {
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

            List<TeachingProgressPO> result = service.listByTask(10L);

            assertThat(result).isEmpty();
            verify(jdbc, never()).queryForList(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("enrich 应回填 courseName / orgUnitName / teacherName")
        void shouldEnrichNames() {
            List<TeachingProgressPO> rows = new ArrayList<>(List.of(row(1L, 10L, 9L, 99L)));
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rows);

            Map<String, Object> courseRow = new LinkedHashMap<>();
            courseRow.put("id", 10L);
            courseRow.put("course_name", "高等数学");
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of(courseRow));
            // batchName -> jdbc.query with RowCallbackHandler
            doAnswerBatchNames();

            List<TeachingProgressPO> result = service.listByTask(10L);

            TeachingProgressPO p = result.get(0);
            assertThat(p.getCourseName()).isEqualTo("高等数学");
            assertThat(p.getOrgUnitName()).isEqualTo("org-9");
            assertThat(p.getTeacherName()).isEqualTo("teacher-99");
        }

        @Test
        @DisplayName("enrich 中 jdbc 抛异常被吞 — 不影响主流程")
        void shouldSwallowEnrichErrors() {
            List<TeachingProgressPO> rows = new ArrayList<>(List.of(row(1L, 10L, 9L, 99L)));
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rows);
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenThrow(new RuntimeException("db down"));
            lenient().doThrow(new RuntimeException("db down"))
                    .when(jdbc).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));

            List<TeachingProgressPO> result = service.listByTask(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCourseName()).isNull();
        }

        private void doAnswerBatchNames() {
            lenient().doAnswer(inv -> {
                String sql = inv.getArgument(0);
                RowCallbackHandler handler = inv.getArgument(1);
                java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                if (sql.contains("org_units")) {
                    when(rs.getLong("id")).thenReturn(9L);
                    when(rs.getString("name")).thenReturn("org-9");
                } else {
                    when(rs.getLong("id")).thenReturn(99L);
                    when(rs.getString("name")).thenReturn("teacher-99");
                }
                handler.processRow(rs);
                return null;
            }).when(jdbc).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
        }
    }

    @Nested
    @DisplayName("listBySemester 按学期查询")
    class ListBySemesterTests {

        @Test
        @DisplayName("仅 semesterId — 查询并返回")
        void shouldListBySemesterOnly() {
            when(mapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(new ArrayList<>());

            List<TeachingProgressPO> result = service.listBySemester(1L, null, null, null);

            assertThat(result).isEmpty();
            verify(mapper).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("带 orgUnitId/weekNumber/status 全过滤 — 查询并 enrich")
        void shouldListWithAllFilters() {
            List<TeachingProgressPO> rows = List.of(row(1L, 10L, 9L, 99L));
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rows);
            stubEnrichEmpty();

            List<TeachingProgressPO> result = service.listBySemester(1L, 9L, 3, 1);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("create 创建进度记录")
    class CreateTests {

        @Test
        @DisplayName("taskId 为空 — 抛 TeachingDomainException")
        void shouldThrowWhenNoTaskId() {
            TeachingProgressPO body = new TeachingProgressPO();

            assertThatThrownBy(() -> service.create(body, 99L))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("taskId 不能为空");
        }

        @Test
        @DisplayName("semesterId/orgUnitId 已提供 — 不查 teaching_tasks, 直接 insert")
        void shouldInsertWithoutDerivation() {
            TeachingProgressPO body = new TeachingProgressPO();
            body.setTaskId(10L);
            body.setSemesterId(1L);
            body.setOrgUnitId(9L);

            TeachingProgressPO result = service.create(body, 77L);

            assertThat(result.getProgressStatus()).isEqualTo(0);
            assertThat(result.getLessonNo()).isEqualTo(1);
            assertThat(result.getRecordedBy()).isEqualTo(77L);
            assertThat(result.getCreatedBy()).isEqualTo(77L);
            verify(jdbc, never()).queryForMap(anyString(), any());
            verify(mapper).insert(body);
        }

        @Test
        @DisplayName("semesterId 缺失 — 从 teaching_tasks 派生 semesterId + orgUnitId")
        void shouldDeriveFromTask() {
            TeachingProgressPO body = new TeachingProgressPO();
            body.setTaskId(10L);

            Map<String, Object> task = new LinkedHashMap<>();
            task.put("semester_id", 5L);
            task.put("org_unit_id", 8L);
            when(jdbc.queryForMap(anyString(), eq(10L))).thenReturn(task);

            TeachingProgressPO result = service.create(body, 77L);

            assertThat(result.getSemesterId()).isEqualTo(5L);
            assertThat(result.getOrgUnitId()).isEqualTo(8L);
            verify(mapper).insert(body);
        }

        @Test
        @DisplayName("派生时 org_unit_id 为 null — 仅回填 semesterId")
        void shouldDeriveSemesterOnlyWhenOrgNull() {
            TeachingProgressPO body = new TeachingProgressPO();
            body.setTaskId(10L);

            Map<String, Object> task = new LinkedHashMap<>();
            task.put("semester_id", 5L);
            task.put("org_unit_id", null);
            when(jdbc.queryForMap(anyString(), eq(10L))).thenReturn(task);

            TeachingProgressPO result = service.create(body, 77L);

            assertThat(result.getSemesterId()).isEqualTo(5L);
            assertThat(result.getOrgUnitId()).isNull();
        }

        @Test
        @DisplayName("teaching_tasks 查询失败 — 抛 taskNotFound 异常")
        void shouldThrowWhenTaskNotFound() {
            TeachingProgressPO body = new TeachingProgressPO();
            body.setTaskId(404L);
            when(jdbc.queryForMap(anyString(), eq(404L)))
                    .thenThrow(new RuntimeException("empty result"));

            assertThatThrownBy(() -> service.create(body, 77L))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("404");
        }

        @Test
        @DisplayName("progressStatus=1 且 recordedAt 为空 — 自动补 recordedAt")
        void shouldSetRecordedAtWhenCompleted() {
            TeachingProgressPO body = new TeachingProgressPO();
            body.setTaskId(10L);
            body.setSemesterId(1L);
            body.setOrgUnitId(9L);
            body.setProgressStatus(1);

            TeachingProgressPO result = service.create(body, 77L);

            assertThat(result.getRecordedAt()).isNotNull();
        }

        @Test
        @DisplayName("已提供 recordedBy / lessonNo — 不被默认值覆盖")
        void shouldKeepProvidedValues() {
            TeachingProgressPO body = new TeachingProgressPO();
            body.setTaskId(10L);
            body.setSemesterId(1L);
            body.setOrgUnitId(9L);
            body.setLessonNo(3);
            body.setRecordedBy(555L);

            TeachingProgressPO result = service.create(body, 77L);

            assertThat(result.getLessonNo()).isEqualTo(3);
            assertThat(result.getRecordedBy()).isEqualTo(555L);
            assertThat(result.getCreatedBy()).isEqualTo(77L);
        }
    }

    @Nested
    @DisplayName("update 更新进度记录")
    class UpdateTests {

        @Test
        @DisplayName("记录不存在 — 抛 TeachingDomainException")
        void shouldThrowWhenNotFound() {
            when(mapper.selectById(404L)).thenReturn(null);

            assertThatThrownBy(() -> service.update(404L, new TeachingProgressPO(), 77L))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("教学进度记录不存在: 404");
        }

        @Test
        @DisplayName("部分字段更新 — 仅非空字段被覆盖")
        void shouldUpdatePartialFields() {
            TeachingProgressPO existing = row(1L, 10L, 9L, 99L);
            existing.setActualTopic("旧主题");
            existing.setNote("旧备注");
            when(mapper.selectById(1L)).thenReturn(existing);

            TeachingProgressPO body = new TeachingProgressPO();
            body.setActualTopic("新主题");
            body.setChapter("第3章");
            body.setAttendanceCount(40);
            body.setTotalStudents(45);

            TeachingProgressPO result = service.update(1L, body, 77L);

            assertThat(result.getActualTopic()).isEqualTo("新主题");
            assertThat(result.getChapter()).isEqualTo("第3章");
            assertThat(result.getAttendanceCount()).isEqualTo(40);
            assertThat(result.getTotalStudents()).isEqualTo(45);
            assertThat(result.getNote()).isEqualTo("旧备注");
            verify(mapper).updateById(existing);
        }

        @Test
        @DisplayName("progressStatus=1 且原 recordedAt 为空 — 补 recordedAt + recordedBy")
        void shouldSetRecordedWhenCompleted() {
            TeachingProgressPO existing = row(1L, 10L, 9L, 99L);
            existing.setRecordedAt(null);
            existing.setRecordedBy(null);
            when(mapper.selectById(1L)).thenReturn(existing);

            TeachingProgressPO body = new TeachingProgressPO();
            body.setProgressStatus(1);

            TeachingProgressPO result = service.update(1L, body, 77L);

            assertThat(result.getProgressStatus()).isEqualTo(1);
            assertThat(result.getRecordedAt()).isNotNull();
            assertThat(result.getRecordedBy()).isEqualTo(77L);
        }

        @Test
        @DisplayName("progressStatus=2 — 改状态但不补 recordedAt")
        void shouldNotSetRecordedForNonCompleted() {
            TeachingProgressPO existing = row(1L, 10L, 9L, 99L);
            existing.setRecordedAt(null);
            when(mapper.selectById(1L)).thenReturn(existing);

            TeachingProgressPO body = new TeachingProgressPO();
            body.setProgressStatus(2);

            TeachingProgressPO result = service.update(1L, body, 77L);

            assertThat(result.getProgressStatus()).isEqualTo(2);
            assertThat(result.getRecordedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("delete 删除")
    class DeleteTests {

        @Test
        @DisplayName("应委托 mapper.deleteById")
        void shouldDelete() {
            service.delete(50L);
            verify(mapper).deleteById(50L);
        }
    }

    @Nested
    @DisplayName("taskSummary 任务完成度统计")
    class TaskSummaryTests {

        @Test
        @DisplayName("有各状态记录 — 正确汇总并计算完成率")
        void shouldComputeSummary() {
            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(statusRow(0, 2));
            rows.add(statusRow(1, 6));
            rows.add(statusRow(2, 1));
            rows.add(statusRow(3, 1));
            when(jdbc.queryForList(anyString(), eq(10L))).thenReturn(rows);

            Map<String, Object> result = service.taskSummary(10L);

            assertThat(result.get("taskId")).isEqualTo(10L);
            assertThat(result.get("totalLessons")).isEqualTo(10);
            assertThat(result.get("completed")).isEqualTo(6);
            assertThat(result.get("missed")).isEqualTo(1);
            assertThat(result.get("adjusted")).isEqualTo(1);
            assertThat(result.get("completionRate")).isEqualTo(60L);
            assertThat(result).doesNotContainKey("error");
        }

        @Test
        @DisplayName("无记录 — 完成率为 0")
        void shouldHandleNoRows() {
            when(jdbc.queryForList(anyString(), eq(10L))).thenReturn(new ArrayList<>());

            Map<String, Object> result = service.taskSummary(10L);

            assertThat(result.get("totalLessons")).isEqualTo(0);
            // completionRate 三元表达式含 Math.round(long) → 整体类型 long, 返回 Long
            assertThat(result.get("completionRate")).isEqualTo(0L);
        }

        @Test
        @DisplayName("jdbc 抛异常 — 结果含 error 字段")
        void shouldCaptureError() {
            when(jdbc.queryForList(anyString(), eq(10L)))
                    .thenThrow(new RuntimeException("query failed"));

            Map<String, Object> result = service.taskSummary(10L);

            assertThat(result.get("taskId")).isEqualTo(10L);
            assertThat(result.get("error")).isEqualTo("query failed");
        }

        @Test
        @DisplayName("queryForList 应传入 task_id 参数")
        void shouldPassTaskIdParam() {
            when(jdbc.queryForList(anyString(), eq(10L))).thenReturn(new ArrayList<>());

            service.taskSummary(10L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), eq(10L));
            assertThat(sqlCap.getValue()).contains("GROUP BY progress_status");
        }

        private Map<String, Object> statusRow(int status, int cnt) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("progress_status", status);
            m.put("cnt", cnt);
            return m;
        }
    }
}
