package com.school.management.infrastructure.extension.plugins.education.application.teaching.saga;

import com.school.management.infrastructure.extension.plugins.education.application.teaching.TeachingWorkflowService;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.event.ExamBatchPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 考试批次发布 Saga: 发布事务 commit 后, 异步补齐缺失的考试安排.
 *
 * AFTER_COMMIT 保证看到已发布状态; fallbackExecution=true 让无事务上下文也能工作.
 * Propagation.REQUIRES_NEW 让本步骤独立事务, 失败不回滚发布动作 (Saga 语义).
 *
 * 失败处理:
 *   单 task 生成失败 → 记录日志继续, 不阻塞其它 task (partial-success).
 *   全部失败 → 仅日志 ERROR, 由人工介入或后续手动 generateExamArrangements 兜底.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamPublishedSagaHandler {

    private final TeachingWorkflowService workflowService;
    private final JdbcTemplate jdbc;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExamBatchPublished(ExamBatchPublishedEvent event) {
        log.info("[Saga/ExamPublished] batchId={} batchName={} semesterId={}",
                event.getBatchId(), event.getBatchName(), event.getSemesterId());

        try {
            // 找出该学期内还没排考试安排的 task
            List<Map<String, Object>> missing = jdbc.queryForList(
                "SELECT t.id FROM teaching_tasks t " +
                "WHERE t.semester_id = ? AND t.deleted = 0 " +
                "  AND NOT EXISTS (SELECT 1 FROM exam_arrangements ea " +
                "                  WHERE ea.batch_id = ? AND ea.task_id = t.id)",
                event.getSemesterId(), event.getBatchId());

            if (missing.isEmpty()) {
                log.info("[Saga/ExamPublished] batchId={} 无遗漏 task, 跳过自动补齐", event.getBatchId());
                return;
            }

            List<Long> taskIds = new ArrayList<>(missing.size());
            for (Map<String, Object> row : missing) {
                Object idObj = row.get("id");
                if (idObj != null) taskIds.add(((Number) idObj).longValue());
            }

            Long publishedBy = event.getPublishedBy() != null ? event.getPublishedBy() : 0L;
            int created = workflowService.generateExamFromTasks(event.getBatchId(), taskIds, publishedBy);
            log.info("[Saga/ExamPublished] batchId={} 自动补齐 {} 条考试安排", event.getBatchId(), created);
        } catch (Exception e) {
            // Saga 语义: 这一步失败不影响主事务 (主事务已 commit), 仅日志告警
            log.error("[Saga/ExamPublished] batchId={} 自动补齐失败, 需人工介入: {}",
                    event.getBatchId(), e.getMessage(), e);
        }
    }
}
