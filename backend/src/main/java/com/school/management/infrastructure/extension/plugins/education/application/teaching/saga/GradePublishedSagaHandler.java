package com.school.management.infrastructure.extension.plugins.education.application.teaching.saga;

import com.school.management.infrastructure.extension.plugins.education.domain.teaching.event.GradeBatchPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

/**
 * 成绩批次发布 Saga: 发布事务 commit 后:
 *   1. 不及格学生预警 — 写入 student_alerts (如表存在)
 *   2. 重算学分 (留扩展点)
 *   3. 触发审计日志
 *
 * Saga 语义: 主事务已 commit, 这里失败仅日志, 不回滚发布动作.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GradePublishedSagaHandler {

    private final JdbcTemplate jdbc;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onGradeBatchPublished(GradeBatchPublishedEvent event) {
        log.info("[Saga/GradePublished] batchId={} batchName={} semesterId={} courseId={} orgUnitId={}",
                event.getBatchId(), event.getBatchName(), event.getSemesterId(),
                event.getCourseId(), event.getOrgUnitId());

        // Step 1: 统计本批不及格学生
        try {
            List<Map<String, Object>> failed = jdbc.queryForList(
                "SELECT student_id, total_score FROM student_grades " +
                "WHERE batch_id = ? AND deleted = 0 " +
                "  AND total_score IS NOT NULL AND total_score < 60",
                event.getBatchId());
            if (!failed.isEmpty()) {
                log.warn("[Saga/GradePublished] batchId={} 检测到 {} 名学生不及格",
                        event.getBatchId(), failed.size());
                // 这里仅记录, 真实的 alerts 表写入由后续 listener 接续
            }
        } catch (Exception e) {
            log.error("[Saga/GradePublished] 不及格统计失败 batchId={}: {}",
                    event.getBatchId(), e.getMessage());
        }

        // Step 2: 审计 — 写入 inspection_audit_logs (如果表存在)
        try {
            jdbc.update(
                "INSERT INTO inspection_audit_logs " +
                "(target_type, target_id, action, detail, operator_id, created_at) " +
                "VALUES ('grade_batch', ?, 'PUBLISHED', ?, ?, NOW())",
                event.getBatchId(),
                String.format("{\"batchName\":\"%s\",\"semesterId\":%d,\"courseId\":%s}",
                        event.getBatchName() != null ? event.getBatchName() : "",
                        event.getSemesterId() != null ? event.getSemesterId() : 0L,
                        event.getCourseId() != null ? event.getCourseId() : "null"),
                event.getPublishedBy() != null ? event.getPublishedBy() : 0L);
        } catch (Exception e) {
            // 审计表可能不存在或字段不匹配, 仅记录
            log.debug("[Saga/GradePublished] 审计写入跳过: {}", e.getMessage());
        }
    }
}
