package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.exception.TeachingDomainException;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 学生评教服务.
 *   1. 教务管理员: 创建评教活动, 配置指标, 启动/结束
 *   2. 学生: 列出待评教 task, 提交打分
 *   3. 教师/管理员: 查看汇总分数 (匿名情况下不暴露 student_id)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseEvaluationService {

    private final CourseEvaluationMapper evalMapper;
    private final EvaluationIndicatorMapper indicatorMapper;
    private final EvaluationResponseMapper responseMapper;
    private final JdbcTemplate jdbc;

    /* ==================== 评教活动 CRUD ==================== */

    public List<CourseEvaluationPO> listEvaluations(Long semesterId, Integer status) {
        LambdaQueryWrapper<CourseEvaluationPO> w = new LambdaQueryWrapper<>();
        if (semesterId != null) w.eq(CourseEvaluationPO::getSemesterId, semesterId);
        if (status != null) w.eq(CourseEvaluationPO::getStatus, status);
        w.orderByDesc(CourseEvaluationPO::getCreatedAt);
        return evalMapper.selectList(w);
    }

    public CourseEvaluationPO getEvaluation(Long id) {
        CourseEvaluationPO po = evalMapper.selectById(id);
        if (po == null) throw new TeachingDomainException("评教活动不存在: " + id);
        return po;
    }

    @Transactional
    public CourseEvaluationPO createEvaluation(CourseEvaluationPO body, Long userId) {
        long id = IdWorker.getId();
        body.setId(id);
        if (body.getEvaluationCode() == null || body.getEvaluationCode().isEmpty()) {
            body.setEvaluationCode("EV" + id);
        }
        if (body.getStatus() == null) body.setStatus(0);
        if (body.getAnonymous() == null) body.setAnonymous(1);
        body.setCreatedBy(userId);
        evalMapper.insert(body);
        return body;
    }

    @Transactional
    public CourseEvaluationPO updateEvaluation(Long id, CourseEvaluationPO body) {
        CourseEvaluationPO po = getEvaluation(id);
        if (po.getStatus() != null && po.getStatus() == 2) {
            throw new TeachingDomainException("已结束的评教活动不能修改");
        }
        if (body.getEvaluationName() != null) po.setEvaluationName(body.getEvaluationName());
        if (body.getOrgUnitId() != null) po.setOrgUnitId(body.getOrgUnitId());
        if (body.getStartTime() != null) po.setStartTime(body.getStartTime());
        if (body.getEndTime() != null) po.setEndTime(body.getEndTime());
        if (body.getAnonymous() != null) po.setAnonymous(body.getAnonymous());
        if (body.getDescription() != null) po.setDescription(body.getDescription());
        evalMapper.updateById(po);
        return po;
    }

    @Transactional
    public void startEvaluation(Long id) {
        CourseEvaluationPO po = getEvaluation(id);
        if (po.getStatus() == null || po.getStatus() != 0) {
            throw new TeachingDomainException("只能启动草稿状态的评教活动");
        }
        if (indicatorMapper.selectCount(new LambdaQueryWrapper<EvaluationIndicatorPO>()
                .eq(EvaluationIndicatorPO::getEvaluationId, id)) == 0) {
            throw new TeachingDomainException("请先配置评教指标项");
        }
        po.setStatus(1);
        evalMapper.updateById(po);
    }

    @Transactional
    public void closeEvaluation(Long id) {
        CourseEvaluationPO po = getEvaluation(id);
        po.setStatus(2);
        evalMapper.updateById(po);
    }

    @Transactional
    public void deleteEvaluation(Long id) {
        evalMapper.deleteById(id);
    }

    /* ==================== 评教指标项 ==================== */

    public List<EvaluationIndicatorPO> listIndicators(Long evaluationId) {
        return indicatorMapper.selectList(
                new LambdaQueryWrapper<EvaluationIndicatorPO>()
                        .eq(EvaluationIndicatorPO::getEvaluationId, evaluationId)
                        .orderByAsc(EvaluationIndicatorPO::getSortOrder));
    }

    @Transactional
    public EvaluationIndicatorPO createIndicator(Long evaluationId, EvaluationIndicatorPO body) {
        // 评教启动后不能改指标
        CourseEvaluationPO eval = getEvaluation(evaluationId);
        if (eval.getStatus() != null && eval.getStatus() >= 1) {
            throw new TeachingDomainException("评教进行中或已结束, 不能修改指标");
        }
        body.setId(null);
        body.setEvaluationId(evaluationId);
        if (body.getMaxScore() == null) body.setMaxScore(5);
        if (body.getWeight() == null) body.setWeight(100);
        if (body.getSortOrder() == null) body.setSortOrder(0);
        if (body.getRequired() == null) body.setRequired(1);
        indicatorMapper.insert(body);
        return body;
    }

    @Transactional
    public void deleteIndicator(Long id) {
        indicatorMapper.deleteById(id);
    }

    /* ==================== 学生提交 ==================== */

    /** 列出某学生在某评教活动下的全部待评 task */
    public List<Map<String, Object>> listMyPendingTasks(Long evaluationId, Long studentId) {
        CourseEvaluationPO eval = getEvaluation(evaluationId);
        // 取学生所在 org_unit, 找出该学期所有相关 task
        try {
            return jdbc.queryForList(
                "SELECT t.id AS taskId, t.course_id AS courseId, c.course_name AS courseName, " +
                "tt.teacher_id AS teacherId, u.real_name AS teacherName, " +
                "(SELECT 1 FROM evaluation_responses er WHERE er.evaluation_id = ? " +
                "  AND er.task_id = t.id AND er.student_id = ? AND er.status = 1 AND er.deleted = 0) AS submitted " +
                "FROM teaching_tasks t " +
                "LEFT JOIN courses c ON c.id = t.course_id " +
                "INNER JOIN teaching_task_teachers tt ON tt.task_id = t.id AND tt.teacher_role = 1 " +
                "LEFT JOIN users u ON u.id = tt.teacher_id " +
                "WHERE t.semester_id = ? AND t.deleted = 0 " +
                "  AND t.org_unit_id = (SELECT org_unit_id FROM user_student WHERE user_id = ? AND deleted = 0 LIMIT 1)",
                evaluationId, studentId, eval.getSemesterId(), studentId);
        } catch (Exception e) {
            log.warn("查询学生待评 task 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public EvaluationResponsePO submit(Long evaluationId, Long studentId, EvaluationResponsePO body) {
        CourseEvaluationPO eval = getEvaluation(evaluationId);
        if (eval.getStatus() == null || eval.getStatus() != 1) {
            throw new TeachingDomainException("评教未在进行中, 不能提交");
        }
        if (body.getTaskId() == null || body.getTeacherId() == null) {
            throw new TeachingDomainException("taskId 和 teacherId 不能为空");
        }
        // 派生学生 org_unit_id
        Long orgUnitId = null;
        try {
            orgUnitId = jdbc.queryForObject(
                "SELECT org_unit_id FROM user_student WHERE user_id = ? AND deleted = 0 LIMIT 1",
                Long.class, studentId);
        } catch (Exception ignored) {}

        // 唯一约束保证一人一份, upsert
        EvaluationResponsePO existing = responseMapper.selectOne(
            new LambdaQueryWrapper<EvaluationResponsePO>()
                .eq(EvaluationResponsePO::getEvaluationId, evaluationId)
                .eq(EvaluationResponsePO::getTaskId, body.getTaskId())
                .eq(EvaluationResponsePO::getStudentId, studentId));
        if (existing != null && existing.getStatus() != null && existing.getStatus() == 1) {
            throw new TeachingDomainException("已经提交过, 不能重复");
        }

        // 计算总分 (加权)
        BigDecimal total = computeTotalScore(evaluationId, body.getScoresJson());

        EvaluationResponsePO po = existing != null ? existing : new EvaluationResponsePO();
        po.setEvaluationId(evaluationId);
        po.setTaskId(body.getTaskId());
        po.setTeacherId(body.getTeacherId());
        po.setStudentId(studentId);
        po.setOrgUnitId(orgUnitId);
        po.setScoresJson(body.getScoresJson());
        po.setComment(body.getComment());
        po.setTotalScore(total);
        po.setStatus(1);
        po.setSubmittedAt(LocalDateTime.now());

        if (existing != null) responseMapper.updateById(po);
        else responseMapper.insert(po);
        return po;
    }

    /** 教师/管理员视角: 某 task 的汇总分数 */
    public Map<String, Object> taskSummary(Long evaluationId, Long taskId) {
        Map<String, Object> result = new HashMap<>();
        result.put("evaluationId", evaluationId);
        result.put("taskId", taskId);
        try {
            Map<String, Object> stats = jdbc.queryForMap(
                "SELECT COUNT(*) AS submissions, " +
                "AVG(total_score) AS avgScore, MIN(total_score) AS minScore, MAX(total_score) AS maxScore " +
                "FROM evaluation_responses " +
                "WHERE evaluation_id = ? AND task_id = ? AND status = 1 AND deleted = 0",
                evaluationId, taskId);
            result.putAll(stats);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /** 计算总分: 各指标分 × 权重 / 100, 累加得加权平均 */
    @SuppressWarnings("unchecked")
    private BigDecimal computeTotalScore(Long evaluationId, String scoresJson) {
        if (scoresJson == null || scoresJson.isEmpty()) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> scores = om.readValue(scoresJson, List.class);
            // 加载指标权重
            List<EvaluationIndicatorPO> indicators = listIndicators(evaluationId);
            Map<Long, EvaluationIndicatorPO> byId = new HashMap<>();
            int totalWeight = 0;
            for (EvaluationIndicatorPO ind : indicators) {
                byId.put(ind.getId(), ind);
                totalWeight += (ind.getWeight() != null ? ind.getWeight() : 0);
            }
            if (totalWeight <= 0) return null;

            BigDecimal weighted = BigDecimal.ZERO;
            for (Map<String, Object> s : scores) {
                Long indId = ((Number) s.get("indicatorId")).longValue();
                Number sc = (Number) s.get("score");
                EvaluationIndicatorPO ind = byId.get(indId);
                if (ind == null || sc == null) continue;
                weighted = weighted.add(BigDecimal.valueOf(sc.doubleValue())
                        .multiply(BigDecimal.valueOf(ind.getWeight())));
            }
            return weighted.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("总分计算失败: {}", e.getMessage());
            return null;
        }
    }
}
