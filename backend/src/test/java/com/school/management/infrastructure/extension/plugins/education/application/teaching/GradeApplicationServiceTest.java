package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.application.event.TriggerService;
import com.school.management.exception.TeachingDomainException;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.grade.GradeBatchMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.grade.GradeBatchPO;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.grade.StudentGradeMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.grade.StudentGradePO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * GradeApplicationService 单测 — 验证成绩批次/成绩 CRUD、统计、排名、权重计算的行为
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GradeApplicationService 测试")
class GradeApplicationServiceTest {

    @Mock
    private GradeBatchMapper batchMapper;
    @Mock
    private StudentGradeMapper gradeMapper;
    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private ApplicationEventPublisher events;
    @Mock
    private TriggerService triggerService;
    @Mock
    private com.school.management.infrastructure.access.OrgScopeHelper orgScopeHelper;

    @InjectMocks
    private GradeApplicationService service;

    @BeforeEach
    void injectTriggerService() {
        ReflectionTestUtils.setField(service, "triggerService", triggerService);
        // 数据权限 helper: 默认放行 (空 clause / org 允许), 保持既有测试 SQL 断言不变
        lenient().when(orgScopeHelper.orgScopeClause(anyString())).thenReturn("");
        lenient().when(orgScopeHelper.isOrgAllowed(any())).thenReturn(true);
    }

    private GradeBatchPO batchWithStatus(int status) {
        GradeBatchPO po = new GradeBatchPO();
        po.setId(100L);
        po.setBatchName("期末批次");
        po.setSemesterId(1L);
        po.setCourseId(2L);
        po.setOrgUnitId(3L);
        po.setStatus(status);
        po.setCreatedBy(9L);
        return po;
    }

    // ==================== Batch listing ====================

    @Nested
    @DisplayName("批次列表与查询")
    class BatchListing {

        @Test
        @DisplayName("listBatches 返回 records 与 total")
        @SuppressWarnings("unchecked")
        void shouldReturnRecordsAndTotal() {
            GradeBatchPO po = batchWithStatus(0);
            Page<GradeBatchPO> page = new Page<>(1, 10);
            page.setRecords(List.of(po));
            page.setTotal(1L);
            when(batchMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Map<String, Object> result = service.listBatches(1L, 2, 0, 1, 10);

            assertThat(result.get("total")).isEqualTo(1L);
            assertThat((List<GradeBatchPO>) result.get("records")).containsExactly(po);
            // enrichBatchNames issues 3 fetchNameMap jdbc.query calls (course/org/user)
            verify(jdbc, times(3)).query(anyString(), any(Object[].class), any(org.springframework.jdbc.core.RowCallbackHandler.class));
        }

        @Test
        @DisplayName("listBatches 空结果时不触发名称补全查询")
        @SuppressWarnings("unchecked")
        void shouldSkipEnrichWhenEmpty() {
            Page<GradeBatchPO> page = new Page<>(1, 10);
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
            GradeBatchPO po = batchWithStatus(0);
            when(batchMapper.selectById(100L)).thenReturn(po);

            GradeBatchPO result = service.getBatch(100L);

            assertThat(result).isSameAs(po);
            verify(jdbc, times(3)).query(anyString(), any(Object[].class), any(org.springframework.jdbc.core.RowCallbackHandler.class));
        }

        @Test
        @DisplayName("getBatch 未命中返回 null 且不查询")
        void shouldReturnNullWhenBatchMissing() {
            when(batchMapper.selectById(404L)).thenReturn(null);

            assertThat(service.getBatch(404L)).isNull();
            verifyNoInteractions(jdbc);
        }
    }

    // ==================== Batch CRUD ====================

    @Nested
    @DisplayName("批次创建/更新/删除")
    class BatchCrud {

        @Test
        @DisplayName("createBatch 组装 PO 并生成 batchCode")
        void shouldCreateBatch() {
            Map<String, Object> data = new HashMap<>();
            data.put("batchName", "新批次");
            data.put("semesterId", 5);
            data.put("courseId", 6);
            data.put("orgUnitId", 7);
            data.put("gradeType", 2);
            data.put("inputDeadline", "2024-09-01T10:00:00");

            GradeBatchPO po = service.createBatch(data, 88L);

            assertThat(po.getBatchName()).isEqualTo("新批次");
            assertThat(po.getSemesterId()).isEqualTo(5L);
            assertThat(po.getGradeType()).isEqualTo(2);
            assertThat(po.getCreatedBy()).isEqualTo(88L);
            assertThat(po.getBatchCode()).startsWith("GB");
            assertThat(po.getEndTime()).isEqualTo(LocalDateTime.parse("2024-09-01T10:00:00"));
            verify(batchMapper).insert(po);
        }

        @Test
        @DisplayName("createBatch 缺省 gradeType/status 用默认值")
        void shouldUseDefaultsWhenMissing() {
            GradeBatchPO po = service.createBatch(new HashMap<>(), 1L);

            assertThat(po.getGradeType()).isEqualTo(1);
            assertThat(po.getStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("updateBatch 批次不存在抛 TeachingDomainException")
        void shouldThrowWhenUpdateMissingBatch() {
            when(batchMapper.selectById(404L)).thenReturn(null);

            assertThatThrownBy(() -> service.updateBatch(404L, new HashMap<>()))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("404");
        }

        @Test
        @DisplayName("updateBatch 命中时更新并持久化")
        void shouldUpdateBatch() {
            GradeBatchPO po = batchWithStatus(0);
            when(batchMapper.selectById(100L)).thenReturn(po);
            Map<String, Object> data = new HashMap<>();
            data.put("batchName", "改名");
            data.put("status", 1);

            service.updateBatch(100L, data);

            assertThat(po.getBatchName()).isEqualTo("改名");
            assertThat(po.getStatus()).isEqualTo(1);
            verify(batchMapper).updateById(po);
        }

        @Test
        @DisplayName("deleteBatch 物理删除")
        void shouldDeleteBatch() {
            service.deleteBatch(100L);
            verify(batchMapper).deleteById(100L);
        }
    }

    // ==================== Batch state transitions ====================

    @Nested
    @DisplayName("批次状态流转")
    class BatchStateTransitions {

        @Test
        @DisplayName("submitBatch 草稿态转已提交并触发 GRADE_SUBMITTED")
        void shouldSubmitBatch() {
            GradeBatchPO po = batchWithStatus(0);
            when(batchMapper.selectById(100L)).thenReturn(po);

            service.submitBatch(100L);

            assertThat(po.getStatus()).isEqualTo(1);
            verify(batchMapper).updateById(po);
            verify(triggerService).fire(anyString(), any(Map.class));
        }

        @Test
        @DisplayName("submitBatch 批次不存在抛异常")
        void shouldThrowWhenSubmitMissing() {
            when(batchMapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.submitBatch(404L))
                    .isInstanceOf(TeachingDomainException.class);
        }

        @Test
        @DisplayName("submitBatch 非草稿态抛领域异常")
        void shouldThrowWhenSubmitNonDraft() {
            when(batchMapper.selectById(100L)).thenReturn(batchWithStatus(1));
            assertThatThrownBy(() -> service.submitBatch(100L))
                    .isInstanceOf(TeachingDomainException.class);
        }

        @Test
        @DisplayName("approveBatch 已提交态转已审核并触发 GRADE_APPROVED")
        void shouldApproveBatch() {
            GradeBatchPO po = batchWithStatus(1);
            when(batchMapper.selectById(100L)).thenReturn(po);

            service.approveBatch(100L);

            assertThat(po.getStatus()).isEqualTo(2);
            verify(batchMapper).updateById(po);
            verify(triggerService).fire(anyString(), any(Map.class));
        }

        @Test
        @DisplayName("approveBatch 未提交态抛领域异常")
        void shouldThrowWhenApproveNotSubmitted() {
            when(batchMapper.selectById(100L)).thenReturn(batchWithStatus(0));
            assertThatThrownBy(() -> service.approveBatch(100L))
                    .isInstanceOf(TeachingDomainException.class);
        }

        @Test
        @DisplayName("publishBatch 已审核态转已发布并发布领域事件 + 学生级触发")
        void shouldPublishBatch() {
            GradeBatchPO po = batchWithStatus(2);
            when(batchMapper.selectById(100L)).thenReturn(po);
            Map<String, Object> stu = new HashMap<>();
            stu.put("user_id", 50L);
            stu.put("real_name", "张三");
            when(jdbc.queryForList(anyString(), eq(3L))).thenReturn(List.of(stu));

            service.publishBatch(100L);

            assertThat(po.getStatus()).isEqualTo(3);
            verify(batchMapper).updateById(po);
            verify(events).publishEvent(any(Object.class));
            // class-level GRADE_PUBLISHED + per-student GRADE_PUBLISHED_PERSONAL
            verify(triggerService, times(2)).fire(anyString(), any(Map.class));
        }

        @Test
        @DisplayName("publishBatch 未审核态抛领域异常")
        void shouldThrowWhenPublishNotApproved() {
            when(batchMapper.selectById(100L)).thenReturn(batchWithStatus(1));
            assertThatThrownBy(() -> service.publishBatch(100L))
                    .isInstanceOf(TeachingDomainException.class);
        }

        @Test
        @DisplayName("publishBatch orgUnitId 为空时不查询学生")
        void shouldSkipStudentNotifyWhenNoOrgUnit() {
            GradeBatchPO po = batchWithStatus(2);
            po.setOrgUnitId(null);
            when(batchMapper.selectById(100L)).thenReturn(po);

            service.publishBatch(100L);

            assertThat(po.getStatus()).isEqualTo(3);
            verify(jdbc, never()).queryForList(anyString(), any(Object.class));
            verify(triggerService, times(1)).fire(anyString(), any(Map.class));
        }
    }

    // ==================== Grade CRUD ====================

    @Nested
    @DisplayName("成绩记录 CRUD")
    class GradeCrud {

        @Test
        @DisplayName("listGrades 优先走 join 查询")
        void shouldListGradesViaJoin() {
            List<Map<String, Object>> joined = List.of(Map.of("id", 1L));
            when(gradeMapper.listByBatchWithStudentInfo(10L)).thenReturn(joined);

            assertThat(service.listGrades(10L)).isEqualTo(joined);
            verify(gradeMapper, never()).selectList(any());
        }

        @Test
        @DisplayName("listGrades join 失败回退到 selectList")
        void shouldFallbackWhenJoinFails() {
            when(gradeMapper.listByBatchWithStudentInfo(10L)).thenThrow(new RuntimeException("no join"));
            StudentGradePO g = new StudentGradePO();
            g.setId(7L);
            g.setBatchId(10L);
            when(gradeMapper.selectList(any())).thenReturn(List.of(g));

            List<Map<String, Object>> result = service.listGrades(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("id")).isEqualTo(7L);
            assertThat(result.get(0).get("batchId")).isEqualTo(10L);
        }

        @Test
        @DisplayName("recordGrade 组装 PO 并插入")
        void shouldRecordGrade() {
            Map<String, Object> data = new HashMap<>();
            data.put("studentId", 33);
            data.put("courseId", 2);
            data.put("totalScore", "88.5");
            data.put("passed", 1);

            StudentGradePO po = service.recordGrade(99L, data);

            assertThat(po.getBatchId()).isEqualTo(99L);
            assertThat(po.getStudentId()).isEqualTo(33L);
            assertThat(po.getTotalScore()).isEqualByComparingTo("88.5");
            assertThat(po.getGradeStatus()).isEqualTo(1);
            assertThat(po.getDeleted()).isEqualTo(0);
            verify(gradeMapper).insert(po);
        }

        @Test
        @DisplayName("updateGrade 记录不存在抛 TeachingDomainException")
        void shouldThrowWhenUpdateMissingGrade() {
            when(gradeMapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.updateGrade(404L, new HashMap<>()))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("404");
        }

        @Test
        @DisplayName("updateGrade 命中时更新并持久化")
        void shouldUpdateGrade() {
            StudentGradePO po = new StudentGradePO();
            po.setId(7L);
            when(gradeMapper.selectById(7L)).thenReturn(po);
            Map<String, Object> data = new HashMap<>();
            data.put("totalScore", "75");
            data.put("remark", "补录");

            service.updateGrade(7L, data);

            assertThat(po.getTotalScore()).isEqualByComparingTo("75");
            assertThat(po.getRemark()).isEqualTo("补录");
            verify(gradeMapper).updateById(po);
        }

        @Test
        @DisplayName("batchRecordGrades 空列表直接返回不查询批次")
        void shouldReturnEarlyWhenEmpty() {
            service.batchRecordGrades(99L, Collections.emptyList());
            verifyNoInteractions(batchMapper);
            verifyNoInteractions(gradeMapper);
        }

        @Test
        @DisplayName("batchRecordGrades 批次不存在抛异常")
        void shouldThrowWhenBatchMissing() {
            when(batchMapper.selectById(99L)).thenReturn(null);
            assertThatThrownBy(() -> service.batchRecordGrades(99L,
                    List.of(Map.of("studentId", 1))))
                    .isInstanceOf(TeachingDomainException.class);
        }

        @Test
        @DisplayName("batchRecordGrades 缺省 course/org 用批次默认值并计算等级")
        void shouldFillDefaultsAndComputeLevel() {
            GradeBatchPO batch = batchWithStatus(0);
            when(batchMapper.selectById(99L)).thenReturn(batch);
            Map<String, Object> grade = new HashMap<>();
            grade.put("studentId", 1);
            grade.put("totalScore", "92");

            service.batchRecordGrades(99L, List.of(grade));

            ArgumentCaptor<StudentGradePO> cap = ArgumentCaptor.forClass(StudentGradePO.class);
            verify(gradeMapper).insert(cap.capture());
            StudentGradePO po = cap.getValue();
            assertThat(po.getCourseId()).isEqualTo(batch.getCourseId());
            assertThat(po.getOrgUnitId()).isEqualTo(batch.getOrgUnitId());
            assertThat(po.getSemesterId()).isEqualTo(batch.getSemesterId());
            assertThat(po.getGradeLevel()).isEqualTo("A");
        }
    }

    // ==================== Query methods ====================

    @Nested
    @DisplayName("学生/班级成绩查询")
    class QueryMethods {

        @Test
        @DisplayName("getStudentGrades 仅 studentId 时 SQL 不含可选过滤")
        void shouldQueryStudentGradesNoOptional() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.getStudentGrades(5L, null, null);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("g.student_id = ?");
            assertThat(sql).doesNotContain("g.semester_id = ?");
            assertThat(sql).doesNotContain("g.course_id = ?");
        }

        @Test
        @DisplayName("getStudentGrades 带 semester/course 时 SQL 含过滤")
        void shouldQueryStudentGradesWithFilters() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.getStudentGrades(5L, 1L, 2L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> argCap = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).queryForList(sqlCap.capture(), argCap.capture());
            assertThat(sqlCap.getValue()).contains("g.semester_id = ?").contains("g.course_id = ?");
            assertThat(argCap.getValue()).containsExactly(5L, 1L, 2L);
        }

        @Test
        @DisplayName("getStudentGrades join 失败回退到无 join SQL")
        void shouldFallbackStudentGrades() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenThrow(new RuntimeException("join failed"))
                    .thenReturn(Collections.emptyList());

            service.getStudentGrades(5L, null, null);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc, times(2)).queryForList(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getAllValues().get(1)).doesNotContain("LEFT JOIN courses");
        }

        @Test
        @DisplayName("getClassGrades 优先走 join 查询")
        void shouldGetClassGradesViaJoin() {
            List<Map<String, Object>> joined = List.of(Map.of("id", 1L));
            when(gradeMapper.listByClassWithJoins(3L)).thenReturn(joined);

            assertThat(service.getClassGrades(3L)).isEqualTo(joined);
        }

        @Test
        @DisplayName("getClassGrades join 失败回退 selectList")
        void shouldFallbackClassGrades() {
            when(gradeMapper.listByClassWithJoins(3L)).thenThrow(new RuntimeException("fail"));
            StudentGradePO g = new StudentGradePO();
            g.setId(9L);
            when(gradeMapper.selectList(any())).thenReturn(List.of(g));

            List<Map<String, Object>> result = service.getClassGrades(3L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("id")).isEqualTo(9L);
        }
    }

    // ==================== Statistics & Ranking ====================

    @Nested
    @DisplayName("统计与排名")
    class StatisticsAndRanking {

        @Test
        @DisplayName("getStatistics 组合 stats 与 distribution")
        void shouldReturnStatistics() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCount", 10L);
            stats.put("avgScore", new BigDecimal("82.5"));
            stats.put("maxScore", new BigDecimal("99"));
            stats.put("minScore", new BigDecimal("40"));
            stats.put("passedCount", 8L);
            Map<String, Object> dist = Map.of("excellent", 2L, "fail", 1L);
            when(jdbc.queryForMap(anyString(), any(Object[].class)))
                    .thenReturn(stats)
                    .thenReturn(dist);

            Map<String, Object> result = service.getStatistics(1L, 3L, 2L);

            assertThat(result.get("totalCount")).isEqualTo(10L);
            assertThat(result.get("avgScore")).isEqualTo(new BigDecimal("82.5"));
            assertThat(result.get("distribution")).isEqualTo(dist);
        }

        @Test
        @DisplayName("getStatistics 无过滤参数时 SQL where 仅 deleted=0")
        void shouldBuildBaseWhereWhenNoFilters() {
            when(jdbc.queryForMap(anyString(), any(Object[].class)))
                    .thenReturn(new HashMap<>())
                    .thenReturn(new HashMap<>());

            service.getStatistics(null, null, null);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc, times(2)).queryForMap(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getAllValues().get(0)).contains("WHERE deleted = 0")
                    .doesNotContain("batch_id = ?");
        }

        @Test
        @DisplayName("getRanking 走 join 分支并填充 rank 名次")
        void shouldRankWithJoin() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(new HashMap<>(Map.of("studentId", 1L)));
            rows.add(new HashMap<>(Map.of("studentId", 2L)));
            when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(rows);

            List<Map<String, Object>> result = service.getRanking(3L, 1L);

            assertThat(result.get(0).get("rank")).isEqualTo(1);
            assertThat(result.get(1).get("rank")).isEqualTo(2);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).contains("LEFT JOIN user_student");
        }

        @Test
        @DisplayName("getRanking user_student 不存在时走无 join 分支")
        void shouldRankWithoutJoin() {
            when(jdbc.queryForObject(anyString(), eq(Integer.class)))
                    .thenThrow(new RuntimeException("no table"));
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.getRanking(3L, null);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).doesNotContain("LEFT JOIN user_student");
            assertThat(sqlCap.getValue()).doesNotContain("g.semester_id");
        }
    }

    // ==================== Weight config & overall calc ====================

    @Nested
    @DisplayName("权重配置与总评计算")
    class WeightAndOverall {

        @Test
        @DisplayName("getWeightConfigs 传 semester+course 参数查询")
        void shouldGetWeightConfigs() {
            when(jdbc.queryForList(anyString(), eq(1L), eq(2L)))
                    .thenReturn(Collections.emptyList());

            service.getWeightConfigs(1L, 2L);

            verify(jdbc).queryForList(anyString(), eq(1L), eq(2L));
        }

        @Test
        @DisplayName("saveWeightConfigs 权重和非100抛 RuntimeException")
        void shouldRejectInvalidWeightSum() {
            List<Map<String, Object>> configs = List.of(
                    Map.of("componentType", 1, "weightPercent", 30),
                    Map.of("componentType", 2, "weightPercent", 40));

            assertThatThrownBy(() -> service.saveWeightConfigs(1L, 2L, configs))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("70%");
            verify(jdbc, never()).update(anyString(), any(), any());
        }

        @Test
        @DisplayName("saveWeightConfigs 权重和为100时先删后插")
        void shouldSaveWeightConfigs() {
            List<Map<String, Object>> configs = List.of(
                    Map.of("componentType", 1, "weightPercent", 40),
                    Map.of("componentType", 2, "weightPercent", 60));

            service.saveWeightConfigs(1L, 2L, configs);

            verify(jdbc).update(anyString(), eq(1L), eq(2L)); // DELETE
            verify(jdbc, times(2)).update(anyString(), eq(1L), eq(2L), any(), any()); // 2 INSERTs
        }

        @Test
        @DisplayName("calculateOverallGrades 无权重配置抛 RuntimeException")
        void shouldThrowWhenNoWeights() {
            when(jdbc.queryForList(anyString(), eq(1L), eq(2L)))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.calculateOverallGrades(1L, 2L, 9L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("权重");
        }

        @Test
        @DisplayName("calculateOverallGrades 学生分数不全则 skipped, 齐全则 calculated")
        void shouldCalculateOverall() {
            // getWeightConfigs + scores query both call queryForList(sql, semesterId, courseId)
            // with the SAME 2 args — they are discriminated only by SQL content.
            List<Map<String, Object>> weights = List.of(
                    Map.of("component_type", 1, "weight_percent", 40),
                    Map.of("component_type", 2, "weight_percent", 60));
            when(jdbc.queryForList(argThat(sql -> sql != null && sql.contains("grade_weight_configs")), eq(1L), eq(2L)))
                    .thenReturn(weights);

            // student 100 has both types; student 200 only type 1
            List<Map<String, Object>> scores = new ArrayList<>();
            scores.add(Map.of("student_id", 100L, "grade_type", 1,
                    "total_score", new BigDecimal("80"), "org_unit_id", 3L));
            scores.add(Map.of("student_id", 100L, "grade_type", 2,
                    "total_score", new BigDecimal("90"), "org_unit_id", 3L));
            scores.add(Map.of("student_id", 200L, "grade_type", 1,
                    "total_score", new BigDecimal("70"), "org_unit_id", 3L));
            when(jdbc.queryForList(argThat(sql -> sql != null && sql.contains("grade_batches")), eq(1L), eq(2L)))
                    .thenReturn(scores);

            // existing overall batch found
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(1L), eq(2L)))
                    .thenReturn(777L);
            when(gradeMapper.selectOne(any())).thenReturn(null);

            Map<String, Object> result = service.calculateOverallGrades(1L, 2L, 9L);

            assertThat(result.get("batchId")).isEqualTo(777L);
            assertThat(result.get("calculated")).isEqualTo(1);
            assertThat(result.get("skipped")).isEqualTo(1);
            verify(gradeMapper).insert(any(StudentGradePO.class));
        }

        @Test
        @DisplayName("calculateOverallGrades 总评批次不存在时创建新批次")
        void shouldCreateOverallBatchWhenMissing() {
            List<Map<String, Object>> weights = List.of(
                    Map.of("component_type", 1, "weight_percent", 100));
            when(jdbc.queryForList(argThat(sql -> sql != null && sql.contains("grade_weight_configs")), eq(1L), eq(2L)))
                    .thenReturn(weights);
            when(jdbc.queryForList(argThat(sql -> sql != null && sql.contains("grade_batches")), eq(1L), eq(2L)))
                    .thenReturn(Collections.emptyList());
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(1L), eq(2L)))
                    .thenThrow(new RuntimeException("not found"));

            Map<String, Object> result = service.calculateOverallGrades(1L, 2L, 9L);

            assertThat(result.get("calculated")).isEqualTo(0);
            // INSERT grade_batches executed for new overall batch
            verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("calculateOverallGrades 已存在总评成绩记录则更新")
        void shouldUpdateExistingOverallGrade() {
            List<Map<String, Object>> weights = List.of(
                    Map.of("component_type", 1, "weight_percent", 100));
            when(jdbc.queryForList(argThat(sql -> sql != null && sql.contains("grade_weight_configs")), eq(1L), eq(2L)))
                    .thenReturn(weights);
            List<Map<String, Object>> scores = new ArrayList<>();
            scores.add(Map.of("student_id", 100L, "grade_type", 1,
                    "total_score", new BigDecimal("95"), "org_unit_id", 3L));
            when(jdbc.queryForList(argThat(sql -> sql != null && sql.contains("grade_batches")), eq(1L), eq(2L)))
                    .thenReturn(scores);
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(1L), eq(2L)))
                    .thenReturn(777L);
            StudentGradePO existing = new StudentGradePO();
            existing.setId(5L);
            when(gradeMapper.selectOne(any())).thenReturn(existing);

            Map<String, Object> result = service.calculateOverallGrades(1L, 2L, 9L);

            assertThat(result.get("calculated")).isEqualTo(1);
            assertThat(existing.getTotalScore()).isEqualByComparingTo("95.0");
            assertThat(existing.getGradeLevel()).isEqualTo("A");
            verify(gradeMapper).updateById(existing);
        }
    }
}
