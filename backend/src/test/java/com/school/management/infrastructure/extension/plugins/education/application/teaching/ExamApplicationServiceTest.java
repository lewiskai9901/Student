package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.application.event.TriggerService;
import com.school.management.exception.TeachingDomainException;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.event.ExamBatchPublishedEvent;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.exam.ExamArrangementMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.exam.ExamArrangementPO;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.exam.ExamBatchMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.exam.ExamBatchPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ExamApplicationService 单测 — 验证考试批次/安排 CRUD、考场监考分配、冲突检测的行为
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExamApplicationService 测试")
class ExamApplicationServiceTest {

    @Mock
    private ExamBatchMapper batchMapper;
    @Mock
    private ExamArrangementMapper arrangementMapper;
    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private ApplicationEventPublisher events;
    @Mock
    private TriggerService triggerService;
    @Mock
    private com.school.management.infrastructure.access.OrgScopeHelper orgScopeHelper;

    @InjectMocks
    private ExamApplicationService service;

    @BeforeEach
    void injectTriggerService() {
        ReflectionTestUtils.setField(service, "triggerService", triggerService);
        // S3: 数据权限收窄默认放行 — 既有行为断言不受影响
        lenient().when(orgScopeHelper.orgScopeClause(anyString())).thenReturn("");
        lenient().when(orgScopeHelper.isOrgAllowed(any())).thenReturn(true);
    }

    private ExamBatchPO publishableBatch() {
        ExamBatchPO po = new ExamBatchPO();
        po.setId(100L);
        po.setBatchName("期末考试");
        po.setSemesterId(1L);
        po.setExamType(2);
        po.setStartDate(LocalDate.of(2024, 6, 1));
        po.setEndDate(LocalDate.of(2024, 6, 10));
        po.setStatus(0);
        po.setCreatedBy(9L);
        return po;
    }

    // ==================== Batch listing ====================

    @Nested
    @DisplayName("批次列表与查询")
    class BatchListing {

        @Test
        @DisplayName("listBatches 返回 records 与 total 并补全名称")
        @SuppressWarnings("unchecked")
        void shouldReturnRecordsAndTotal() {
            ExamBatchPO po = publishableBatch();
            Page<ExamBatchPO> page = new Page<>(1, 10);
            page.setRecords(List.of(po));
            page.setTotal(1L);
            when(batchMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Map<String, Object> result = service.listBatches(1L, 2, 0, 1, 10);

            assertThat(result.get("total")).isEqualTo(1L);
            assertThat((List<ExamBatchPO>) result.get("records")).containsExactly(po);
            assertThat(po.getName()).isEqualTo("期末考试");
            // enrichBatchNames: semesters + users fetchNameMap
            verify(jdbc, times(2)).query(anyString(), any(Object[].class),
                    any(org.springframework.jdbc.core.RowCallbackHandler.class));
        }

        @Test
        @DisplayName("listBatches 空结果不触发名称补全")
        @SuppressWarnings("unchecked")
        void shouldSkipEnrichWhenEmpty() {
            Page<ExamBatchPO> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0L);
            when(batchMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Map<String, Object> result = service.listBatches(null, null, null, 1, 10);

            assertThat(result.get("total")).isEqualTo(0L);
            verifyNoInteractions(jdbc);
        }

        @Test
        @DisplayName("getBatch 命中时补全名称")
        void shouldGetBatchAndEnrich() {
            ExamBatchPO po = publishableBatch();
            when(batchMapper.selectById(100L)).thenReturn(po);

            ExamBatchPO result = service.getBatch(100L);

            assertThat(result).isSameAs(po);
            assertThat(result.getName()).isEqualTo("期末考试");
        }

        @Test
        @DisplayName("getBatch 未命中返回 null")
        void shouldReturnNullWhenMissing() {
            when(batchMapper.selectById(404L)).thenReturn(null);
            assertThat(service.getBatch(404L)).isNull();
            verifyNoInteractions(jdbc);
        }
    }

    // ==================== Batch CRUD ====================

    @Nested
    @DisplayName("批次创建/更新/删除/发布")
    class BatchCrud {

        @Test
        @DisplayName("createBatch 组装 PO 并生成 batchCode")
        void shouldCreateBatch() {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "中期考试");
            data.put("semesterId", 5);
            data.put("examType", 1);
            data.put("startDate", "2024-04-01");
            data.put("endDate", "2024-04-05");

            ExamBatchPO po = service.createBatch(data, 88L);

            assertThat(po.getBatchName()).isEqualTo("中期考试");
            assertThat(po.getExamType()).isEqualTo(1);
            assertThat(po.getSemesterId()).isEqualTo(5L);
            assertThat(po.getCreatedBy()).isEqualTo(88L);
            assertThat(po.getBatchCode()).startsWith("EX");
            assertThat(po.getStartDate()).isEqualTo(LocalDate.of(2024, 4, 1));
            assertThat(po.getDeleted()).isEqualTo(0);
            verify(batchMapper).insert(po);
        }

        @Test
        @DisplayName("createBatch examType 缺失抛 IllegalArgumentException")
        void shouldRejectMissingExamType() {
            assertThatThrownBy(() -> service.createBatch(new HashMap<>(), 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("考试类型");
            verify(batchMapper, never()).insert(any(ExamBatchPO.class));
        }

        @Test
        @DisplayName("createBatch examType 越界抛 IllegalArgumentException")
        void shouldRejectOutOfRangeExamType() {
            Map<String, Object> data = new HashMap<>();
            data.put("examType", 9);
            assertThatThrownBy(() -> service.createBatch(data, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("updateBatch 批次不存在抛 TeachingDomainException")
        void shouldThrowWhenUpdateMissing() {
            when(batchMapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.updateBatch(404L, new HashMap<>()))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("404");
        }

        @Test
        @DisplayName("updateBatch 命中时更新并持久化")
        void shouldUpdateBatch() {
            ExamBatchPO po = publishableBatch();
            when(batchMapper.selectById(100L)).thenReturn(po);
            Map<String, Object> data = new HashMap<>();
            data.put("batchName", "改名考试");
            data.put("status", 1);

            service.updateBatch(100L, data);

            assertThat(po.getBatchName()).isEqualTo("改名考试");
            assertThat(po.getStatus()).isEqualTo(1);
            verify(batchMapper).updateById(po);
        }

        @Test
        @DisplayName("deleteBatch 逻辑删除")
        void shouldDeleteBatch() {
            service.deleteBatch(100L);
            verify(batchMapper).deleteById(100L);
        }

        @Test
        @DisplayName("publishBatch 草稿态转已发布并发布事件 + 触发 EXAM_PUBLISHED")
        void shouldPublishBatch() {
            ExamBatchPO po = publishableBatch();
            when(batchMapper.selectById(100L)).thenReturn(po);

            service.publishBatch(100L);

            assertThat(po.getStatus()).isEqualTo(2);
            verify(batchMapper).updateById(po);
            ArgumentCaptor<ExamBatchPublishedEvent> evtCap =
                    ArgumentCaptor.forClass(ExamBatchPublishedEvent.class);
            verify(events).publishEvent(evtCap.capture());
            assertThat(evtCap.getValue().getBatchId()).isEqualTo(100L);
            assertThat(evtCap.getValue().getExamType()).isEqualTo(2);
            verify(triggerService).fire(anyString(), any(Map.class));
        }

        @Test
        @DisplayName("publishBatch 批次不存在抛异常")
        void shouldThrowWhenPublishMissing() {
            when(batchMapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.publishBatch(404L))
                    .isInstanceOf(TeachingDomainException.class);
        }

        @Test
        @DisplayName("publishBatch 已发布态抛领域异常")
        void shouldThrowWhenAlreadyPublished() {
            ExamBatchPO po = publishableBatch();
            po.setStatus(2);
            when(batchMapper.selectById(100L)).thenReturn(po);
            assertThatThrownBy(() -> service.publishBatch(100L))
                    .isInstanceOf(TeachingDomainException.class);
            verify(batchMapper, never()).updateById(any(ExamBatchPO.class));
        }

        @Test
        @DisplayName("publishBatch 结束日期早于开始日期抛领域异常")
        void shouldThrowWhenEndBeforeStart() {
            ExamBatchPO po = publishableBatch();
            po.setEndDate(LocalDate.of(2024, 5, 1));
            when(batchMapper.selectById(100L)).thenReturn(po);
            assertThatThrownBy(() -> service.publishBatch(100L))
                    .isInstanceOf(TeachingDomainException.class);
        }
    }

    // ==================== Arrangement Methods ====================

    @Nested
    @DisplayName("考试安排 CRUD")
    class ArrangementCrud {

        @Test
        @DisplayName("listArrangements 优先走带课程名 join 查询")
        void shouldListArrangementsViaJoin() {
            List<Map<String, Object>> joined = List.of(Map.of("id", 1L));
            when(arrangementMapper.listWithCourseName(10L)).thenReturn(joined);

            assertThat(service.listArrangements(10L)).isEqualTo(joined);
            verify(arrangementMapper, never()).selectList(any());
        }

        @Test
        @DisplayName("listArrangements join 失败回退到 selectList")
        void shouldFallbackWhenJoinFails() {
            when(arrangementMapper.listWithCourseName(10L)).thenThrow(new RuntimeException("no join"));
            ExamArrangementPO a = new ExamArrangementPO();
            a.setId(7L);
            a.setBatchId(10L);
            a.setCourseId(3L);
            when(arrangementMapper.selectList(any())).thenReturn(List.of(a));

            List<Map<String, Object>> result = service.listArrangements(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("id")).isEqualTo(7L);
            assertThat(result.get(0).get("courseId")).isEqualTo(3L);
        }

        @Test
        @DisplayName("createArrangement 组装 PO 并插入")
        void shouldCreateArrangement() {
            Map<String, Object> data = new HashMap<>();
            data.put("courseId", 5);
            data.put("examDate", "2024-06-02");
            data.put("startTime", "09:00");
            data.put("endTime", "11:00");
            data.put("duration", 120);

            ExamArrangementPO po = service.createArrangement(10L, data, 88L);

            assertThat(po.getBatchId()).isEqualTo(10L);
            assertThat(po.getCourseId()).isEqualTo(5L);
            assertThat(po.getDuration()).isEqualTo(120);
            assertThat(po.getExamForm()).isEqualTo(1); // default
            assertThat(po.getTotalStudents()).isEqualTo(0); // default
            assertThat(po.getStatus()).isEqualTo(1);
            assertThat(po.getCreatedBy()).isEqualTo(88L);
            verify(arrangementMapper).insert(po);
        }

        @Test
        @DisplayName("updateArrangement 不存在抛 TeachingDomainException")
        void shouldThrowWhenUpdateMissing() {
            when(arrangementMapper.selectOne(any())).thenReturn(null);
            assertThatThrownBy(() -> service.updateArrangement(10L, 404L, new HashMap<>()))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("404");
        }

        @Test
        @DisplayName("updateArrangement 命中时更新并持久化")
        void shouldUpdateArrangement() {
            ExamArrangementPO po = new ExamArrangementPO();
            po.setId(7L);
            po.setBatchId(10L);
            when(arrangementMapper.selectOne(any())).thenReturn(po);
            Map<String, Object> data = new HashMap<>();
            data.put("courseId", 6);
            data.put("remark", "调整");

            service.updateArrangement(10L, 7L, data);

            assertThat(po.getCourseId()).isEqualTo(6L);
            assertThat(po.getRemark()).isEqualTo("调整");
            verify(arrangementMapper).updateById(po);
        }

        @Test
        @DisplayName("deleteArrangement 按 id+batchId 物理删除")
        void shouldDeleteArrangement() {
            service.deleteArrangement(10L, 7L);
            verify(arrangementMapper).delete(any(LambdaQueryWrapper.class));
        }
    }

    // ==================== Room & Invigilator ====================

    @Nested
    @DisplayName("考场与监考分配")
    class RoomAndInvigilator {

        @Test
        @DisplayName("assignRooms 先删后插, roomCode 缺失自动生成")
        void shouldAssignRooms() {
            List<Map<String, Object>> rooms = new ArrayList<>();
            Map<String, Object> r1 = new HashMap<>();
            r1.put("classroomId", 11);
            r1.put("capacity", 30);
            rooms.add(r1); // roomCode missing -> R1
            Map<String, Object> r2 = new HashMap<>();
            r2.put("classroomId", 12);
            r2.put("capacity", 40);
            r2.put("roomCode", "A201");
            rooms.add(r2);

            service.assignRooms(99L, rooms);

            verify(jdbc).update(anyString(), eq(99L)); // DELETE
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc, times(2)).update(sqlCap.capture(),
                    any(), eq(99L), any(), any(), any());
            assertThat(sqlCap.getAllValues().get(0)).contains("INSERT INTO exam_rooms");
        }

        @Test
        @DisplayName("assignRooms rooms 为 null 时仅执行删除")
        void shouldOnlyDeleteWhenRoomsNull() {
            service.assignRooms(99L, null);
            verify(jdbc, times(1)).update(anyString(), eq(99L));
        }

        @Test
        @DisplayName("assignInvigilators 主考为 role=1, 其余 role=2")
        void shouldAssignInvigilatorsWithRoles() {
            List<Number> teacherIds = List.of(50L, 60L);

            service.assignInvigilators(77L, teacherIds, 50L);

            verify(jdbc).update(anyString(), eq(77L)); // DELETE
            ArgumentCaptor<Object> roleCap = ArgumentCaptor.forClass(Object.class);
            verify(jdbc, times(2)).update(anyString(),
                    any(), eq(77L), any(), roleCap.capture());
            // teacher 50 -> role 1 (main), teacher 60 -> role 2
            assertThat(roleCap.getAllValues()).containsExactly(1, 2);
        }

        @Test
        @DisplayName("assignInvigilators teacherIds 为 null 时仅执行删除")
        void shouldOnlyDeleteWhenTeacherIdsNull() {
            service.assignInvigilators(77L, null, null);
            verify(jdbc, times(1)).update(anyString(), eq(77L));
        }
    }

    // ==================== Conflict Detection ====================

    @Nested
    @DisplayName("考试冲突检测")
    class ConflictDetection {

        @Test
        @DisplayName("detectExamConflicts 无冲突时返回空列表")
        void shouldReturnEmptyWhenNoConflict() {
            when(jdbc.queryForList(anyString(), eq(1L), eq(1L)))
                    .thenReturn(Collections.emptyList());

            List<Map<String, Object>> result = service.detectExamConflicts(1L);

            assertThat(result).isEmpty();
            // room conflict + teacher conflict queries
            verify(jdbc, times(2)).queryForList(anyString(), eq(1L), eq(1L));
        }

        @Test
        @DisplayName("detectExamConflicts 返回 ROOM 与 TEACHER 两类冲突")
        void shouldReturnRoomAndTeacherConflicts() {
            Map<String, Object> roomRow = new HashMap<>();
            roomRow.put("arr1_id", 10L);
            roomRow.put("arr2_id", 11L);
            roomRow.put("room_code", "A101");
            roomRow.put("exam_date", "2024-06-01");
            roomRow.put("start1", "09:00");
            roomRow.put("end1", "11:00");
            roomRow.put("start2", "10:00");
            roomRow.put("end2", "12:00");

            Map<String, Object> teacherRow = new HashMap<>();
            teacherRow.put("teacher_id", 50L);
            teacherRow.put("arr1_id", 20L);
            teacherRow.put("arr2_id", 21L);
            teacherRow.put("exam_date", "2024-06-02");
            teacherRow.put("start1", "14:00");
            teacherRow.put("end1", "16:00");
            teacherRow.put("start2", "15:00");
            teacherRow.put("end2", "17:00");

            when(jdbc.queryForList(anyString(), eq(1L), eq(1L)))
                    .thenReturn(List.of(roomRow))
                    .thenReturn(List.of(teacherRow));

            List<Map<String, Object>> result = service.detectExamConflicts(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).get("type")).isEqualTo("ROOM");
            assertThat(result.get(0).get("arrangement1Id")).isEqualTo(10L);
            assertThat((String) result.get(0).get("description")).contains("A101");
            assertThat(result.get(1).get("type")).isEqualTo("TEACHER");
            assertThat(result.get(1).get("arrangement1Id")).isEqualTo(20L);
            assertThat((String) result.get(1).get("description")).contains("50");
        }
    }
}
