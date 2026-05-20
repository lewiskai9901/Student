package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.exception.TeachingDomainException;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation.CourseEvaluationMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation.CourseEvaluationPO;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation.EvaluationIndicatorMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation.EvaluationIndicatorPO;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation.EvaluationResponseMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation.EvaluationResponsePO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CourseEvaluationService 单测 — 验证学生评教活动/指标/提交/汇总逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CourseEvaluationService 测试")
class CourseEvaluationServiceTest {

    @Mock
    private CourseEvaluationMapper evalMapper;

    @Mock
    private EvaluationIndicatorMapper indicatorMapper;

    @Mock
    private EvaluationResponseMapper responseMapper;

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private CourseEvaluationService service;

    private CourseEvaluationPO evaluation(Long id, Integer status) {
        CourseEvaluationPO po = new CourseEvaluationPO();
        po.setId(id);
        po.setStatus(status);
        po.setSemesterId(10L);
        po.setEvaluationName("期末评教");
        return po;
    }

    private EvaluationIndicatorPO indicator(Long id, Integer weight) {
        EvaluationIndicatorPO po = new EvaluationIndicatorPO();
        po.setId(id);
        po.setWeight(weight);
        return po;
    }

    @Nested
    @DisplayName("评教活动 CRUD")
    class EvaluationCrudTests {

        @Test
        @DisplayName("listEvaluations 应按 semesterId/status 组装查询")
        void shouldListEvaluations() {
            when(evalMapper.selectList(any())).thenReturn(Collections.emptyList());

            service.listEvaluations(10L, 1);

            verify(evalMapper).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("getEvaluation 存在时返回对象")
        void shouldGetEvaluation() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 0));

            CourseEvaluationPO po = service.getEvaluation(1L);

            assertThat(po.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getEvaluation 不存在时抛 TeachingDomainException")
        void shouldThrowWhenEvaluationNotFound() {
            when(evalMapper.selectById(1L)).thenReturn(null);

            assertThatThrownBy(() -> service.getEvaluation(1L))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("评教活动不存在");
        }

        @Test
        @DisplayName("createEvaluation 应补全 code/status/anonymous 默认值")
        void shouldCreateEvaluationWithDefaults() {
            CourseEvaluationPO body = new CourseEvaluationPO();
            body.setEvaluationName("新评教");

            CourseEvaluationPO result = service.createEvaluation(body, 99L);

            assertThat(result.getId()).isNotNull();
            assertThat(result.getEvaluationCode()).startsWith("EV");
            assertThat(result.getStatus()).isZero();
            assertThat(result.getAnonymous()).isEqualTo(1);
            assertThat(result.getCreatedBy()).isEqualTo(99L);
            verify(evalMapper).insert(body);
        }

        @Test
        @DisplayName("createEvaluation 已传 code 时保留原值")
        void shouldKeepProvidedCode() {
            CourseEvaluationPO body = new CourseEvaluationPO();
            body.setEvaluationCode("CUSTOM-1");
            body.setStatus(1);
            body.setAnonymous(0);

            CourseEvaluationPO result = service.createEvaluation(body, 99L);

            assertThat(result.getEvaluationCode()).isEqualTo("CUSTOM-1");
            assertThat(result.getStatus()).isEqualTo(1);
            assertThat(result.getAnonymous()).isZero();
        }

        @Test
        @DisplayName("updateEvaluation 已结束的活动应拒绝修改")
        void shouldRejectUpdateWhenClosed() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 2));
            CourseEvaluationPO body = new CourseEvaluationPO();
            body.setEvaluationName("改名");

            assertThatThrownBy(() -> service.updateEvaluation(1L, body))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("已结束");
        }

        @Test
        @DisplayName("updateEvaluation 应仅覆盖非空字段")
        void shouldUpdateOnlyNonNullFields() {
            CourseEvaluationPO existing = evaluation(1L, 0);
            existing.setDescription("原描述");
            when(evalMapper.selectById(1L)).thenReturn(existing);
            CourseEvaluationPO body = new CourseEvaluationPO();
            body.setEvaluationName("新名称");
            // description 为 null → 保留原值

            CourseEvaluationPO result = service.updateEvaluation(1L, body);

            assertThat(result.getEvaluationName()).isEqualTo("新名称");
            assertThat(result.getDescription()).isEqualTo("原描述");
            verify(evalMapper).updateById(existing);
        }

        @Test
        @DisplayName("startEvaluation 非草稿状态应拒绝")
        void shouldRejectStartWhenNotDraft() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));

            assertThatThrownBy(() -> service.startEvaluation(1L))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("草稿");
        }

        @Test
        @DisplayName("startEvaluation 无指标时应拒绝")
        void shouldRejectStartWhenNoIndicators() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 0));
            when(indicatorMapper.selectCount(any())).thenReturn(0L);

            assertThatThrownBy(() -> service.startEvaluation(1L))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("评教指标项");
        }

        @Test
        @DisplayName("startEvaluation 草稿且有指标时置为进行中")
        void shouldStartEvaluation() {
            CourseEvaluationPO po = evaluation(1L, 0);
            when(evalMapper.selectById(1L)).thenReturn(po);
            when(indicatorMapper.selectCount(any())).thenReturn(3L);

            service.startEvaluation(1L);

            assertThat(po.getStatus()).isEqualTo(1);
            verify(evalMapper).updateById(po);
        }

        @Test
        @DisplayName("closeEvaluation 应将状态置为已结束")
        void shouldCloseEvaluation() {
            CourseEvaluationPO po = evaluation(1L, 1);
            when(evalMapper.selectById(1L)).thenReturn(po);

            service.closeEvaluation(1L);

            assertThat(po.getStatus()).isEqualTo(2);
            verify(evalMapper).updateById(po);
        }

        @Test
        @DisplayName("deleteEvaluation 应委托 mapper 删除")
        void shouldDeleteEvaluation() {
            service.deleteEvaluation(1L);
            verify(evalMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("评教指标项")
    class IndicatorTests {

        @Test
        @DisplayName("listIndicators 应按 evaluationId 查询")
        void shouldListIndicators() {
            when(indicatorMapper.selectList(any())).thenReturn(Collections.emptyList());

            service.listIndicators(1L);

            verify(indicatorMapper).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("createIndicator 评教进行中应拒绝")
        void shouldRejectCreateIndicatorWhenRunning() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));

            assertThatThrownBy(() -> service.createIndicator(1L, new EvaluationIndicatorPO()))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("不能修改指标");
        }

        @Test
        @DisplayName("createIndicator 草稿状态应补全默认值并插入")
        void shouldCreateIndicatorWithDefaults() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 0));
            EvaluationIndicatorPO body = new EvaluationIndicatorPO();
            body.setIndicatorName("教学态度");

            EvaluationIndicatorPO result = service.createIndicator(1L, body);

            assertThat(result.getEvaluationId()).isEqualTo(1L);
            assertThat(result.getMaxScore()).isEqualTo(5);
            assertThat(result.getWeight()).isEqualTo(100);
            assertThat(result.getSortOrder()).isZero();
            assertThat(result.getRequired()).isEqualTo(1);
            verify(indicatorMapper).insert(body);
        }

        @Test
        @DisplayName("deleteIndicator 应委托 mapper 删除")
        void shouldDeleteIndicator() {
            service.deleteIndicator(5L);
            verify(indicatorMapper).deleteById(5L);
        }
    }

    @Nested
    @DisplayName("学生提交 — listMyPendingTasks")
    class ListMyPendingTasksTests {

        @Test
        @DisplayName("正常查询返回待评 task 列表")
        void shouldReturnPendingTasks() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));
            when(jdbc.queryForList(anyString(), eq(1L), eq(2001L), eq(10L), eq(2001L)))
                    .thenReturn(List.of(new HashMap<>()));

            List<Map<String, Object>> result = service.listMyPendingTasks(1L, 2001L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("查询异常时返回空列表兜底")
        void shouldReturnEmptyOnQueryError() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));
            when(jdbc.queryForList(anyString(), eq(1L), eq(2001L), eq(10L), eq(2001L)))
                    .thenThrow(new RuntimeException("table missing"));

            List<Map<String, Object>> result = service.listMyPendingTasks(1L, 2001L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("学生提交 — submit")
    class SubmitTests {

        private EvaluationResponsePO body() {
            EvaluationResponsePO b = new EvaluationResponsePO();
            b.setTaskId(300L);
            b.setTeacherId(400L);
            b.setScoresJson("[{\"indicatorId\":1,\"score\":4.5}]");
            b.setComment("讲得好");
            return b;
        }

        @Test
        @DisplayName("评教未进行中应拒绝提交")
        void shouldRejectSubmitWhenNotRunning() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 0));

            assertThatThrownBy(() -> service.submit(1L, 2001L, body()))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("不能提交");
        }

        @Test
        @DisplayName("taskId 或 teacherId 为空应拒绝")
        void shouldRejectWhenTaskOrTeacherNull() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));
            EvaluationResponsePO b = new EvaluationResponsePO();
            b.setTaskId(null);
            b.setTeacherId(400L);

            assertThatThrownBy(() -> service.submit(1L, 2001L, b))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("不能为空");
        }

        @Test
        @DisplayName("已提交过(status=1)应拒绝重复提交")
        void shouldRejectDuplicateSubmit() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(2001L)))
                    .thenReturn(800L);
            EvaluationResponsePO existing = new EvaluationResponsePO();
            existing.setStatus(1);
            when(responseMapper.selectOne(any())).thenReturn(existing);

            assertThatThrownBy(() -> service.submit(1L, 2001L, body()))
                    .isInstanceOf(TeachingDomainException.class)
                    .hasMessageContaining("不能重复");
        }

        @Test
        @DisplayName("首次提交应 insert 并计算加权总分")
        void shouldInsertOnFirstSubmit() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(2001L)))
                    .thenReturn(800L);
            when(responseMapper.selectOne(any())).thenReturn(null);
            // 指标: 权重 100, 总权重 100; score 4.5 → 4.5*100/100 = 4.50
            when(indicatorMapper.selectList(any())).thenReturn(List.of(indicator(1L, 100)));

            EvaluationResponsePO result = service.submit(1L, 2001L, body());

            assertThat(result.getStatus()).isEqualTo(1);
            assertThat(result.getStudentId()).isEqualTo(2001L);
            assertThat(result.getOrgUnitId()).isEqualTo(800L);
            assertThat(result.getTotalScore()).isEqualByComparingTo(new BigDecimal("4.50"));
            assertThat(result.getSubmittedAt()).isNotNull();
            verify(responseMapper).insert(result);
            verify(responseMapper, never()).updateById(any(EvaluationResponsePO.class));
        }

        @Test
        @DisplayName("存在未提交记录(status=0)应走 update 分支")
        void shouldUpdateWhenDraftResponseExists() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(2001L)))
                    .thenReturn(800L);
            EvaluationResponsePO existing = new EvaluationResponsePO();
            existing.setStatus(0);
            when(responseMapper.selectOne(any())).thenReturn(existing);
            when(indicatorMapper.selectList(any())).thenReturn(List.of(indicator(1L, 100)));

            EvaluationResponsePO result = service.submit(1L, 2001L, body());

            assertThat(result).isSameAs(existing);
            verify(responseMapper).updateById(existing);
            verify(responseMapper, never()).insert(any(EvaluationResponsePO.class));
        }

        @Test
        @DisplayName("学生 org_unit 查询异常时 orgUnitId 兜底为 null")
        void shouldFallbackNullOrgUnitWhenQueryFails() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(2001L)))
                    .thenThrow(new RuntimeException("no user_student"));
            when(responseMapper.selectOne(any())).thenReturn(null);
            when(indicatorMapper.selectList(any())).thenReturn(List.of(indicator(1L, 100)));

            EvaluationResponsePO result = service.submit(1L, 2001L, body());

            assertThat(result.getOrgUnitId()).isNull();
            assertThat(result.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("总权重为 0 时总分为 null")
        void shouldHaveNullScoreWhenTotalWeightZero() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(2001L)))
                    .thenReturn(800L);
            when(responseMapper.selectOne(any())).thenReturn(null);
            when(indicatorMapper.selectList(any())).thenReturn(List.of(indicator(1L, 0)));

            EvaluationResponsePO result = service.submit(1L, 2001L, body());

            assertThat(result.getTotalScore()).isNull();
        }

        @Test
        @DisplayName("scoresJson 为空时总分为 null")
        void shouldHaveNullScoreWhenScoresJsonEmpty() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(2001L)))
                    .thenReturn(800L);
            when(responseMapper.selectOne(any())).thenReturn(null);
            EvaluationResponsePO b = body();
            b.setScoresJson("");

            EvaluationResponsePO result = service.submit(1L, 2001L, b);

            assertThat(result.getTotalScore()).isNull();
        }

        @Test
        @DisplayName("scoresJson 非法时总分计算降级为 null")
        void shouldHaveNullScoreWhenScoresJsonInvalid() {
            when(evalMapper.selectById(1L)).thenReturn(evaluation(1L, 1));
            when(jdbc.queryForObject(anyString(), eq(Long.class), eq(2001L)))
                    .thenReturn(800L);
            when(responseMapper.selectOne(any())).thenReturn(null);
            EvaluationResponsePO b = body();
            b.setScoresJson("not-json");

            EvaluationResponsePO result = service.submit(1L, 2001L, b);

            assertThat(result.getTotalScore()).isNull();
        }
    }

    @Nested
    @DisplayName("taskSummary 汇总分数")
    class TaskSummaryTests {

        @Test
        @DisplayName("正常返回提交数与平均分统计")
        void shouldReturnSummary() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("submissions", 12L);
            stats.put("avgScore", new BigDecimal("4.20"));
            when(jdbc.queryForMap(anyString(), eq(1L), eq(300L))).thenReturn(stats);

            Map<String, Object> result = service.taskSummary(1L, 300L);

            assertThat(result.get("evaluationId")).isEqualTo(1L);
            assertThat(result.get("taskId")).isEqualTo(300L);
            assertThat(result.get("submissions")).isEqualTo(12L);
            assertThat(result.get("avgScore")).isEqualTo(new BigDecimal("4.20"));
        }

        @Test
        @DisplayName("查询异常时结果带 error 字段")
        void shouldPutErrorOnQueryFailure() {
            when(jdbc.queryForMap(anyString(), eq(1L), eq(300L)))
                    .thenThrow(new RuntimeException("db down"));

            Map<String, Object> result = service.taskSummary(1L, 300L);

            assertThat(result).containsKey("error");
            assertThat(result.get("error")).isEqualTo("db down");
        }
    }
}
