package com.school.management.application.inspection.listener;

import com.school.management.application.inspection.InspTaskApplicationService;
import com.school.management.domain.inspection.event.AppealApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * V108 TRIGGERED 类型自动触发器 — 监听申诉/告警/投诉等业务事件,
 * 自动创建一个 TRIGGERED 核查任务.
 *
 * <p>设计要点:
 * <ul>
 *   <li>{@code @TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)} —
 *       上游事务提交后才创建 task, 避免读未提交</li>
 *   <li>{@code @Async} — 异步处理, 不阻塞主流程</li>
 *   <li>防重: createTriggeredTask 内部已按 refType+refId 去重</li>
 *   <li>失败安全降级: 任何异常吞掉 + log, 不影响业务主链路</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TriggeredTaskAutoCreateListener {

    private final InspTaskApplicationService taskService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 申诉 APPROVED → 自动创建 TRIGGERED 核查任务.
     * 用于"申诉通过后, 系统应当再次核查该项是否真有问题".
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Async
    public void onAppealApproved(AppealApprovedEvent event) {
        try {
            Long projectId = resolveProjectIdFromAppeal(event.getAppealId());
            if (projectId == null) {
                log.debug("[V108] AppealApproved → 找不到 projectId, 跳过 (appealId={})", event.getAppealId());
                return;
            }
            String reason = String.format("申诉 %s 通过 → 系统自动核查 (originDetailId=%d, finalAdjustment=%s)",
                    event.getAppealCode(), event.getSubmissionDetailId(),
                    event.getFinalAdjustment() != null ? event.getFinalAdjustment().toPlainString() : "0");
            taskService.createTriggeredTask(projectId, "Appeal", event.getAppealId(), reason);
        } catch (Exception e) {
            log.warn("[V108] AppealApproved listener 失败 (appealId={}): {}",
                    event.getAppealId(), e.getMessage());
        }
    }

    /** 从申诉单查 projectId — 沿 appeal → submission_detail → submission → task → project_id 链 */
    private Long resolveProjectIdFromAppeal(Long appealId) {
        if (appealId == null) return null;
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT t.project_id FROM inspection_appeals a " +
                    "JOIN insp_submission_details d ON d.id = a.submission_detail_id " +
                    "JOIN insp_submissions s ON s.id = d.submission_id " +
                    "JOIN insp_tasks t ON t.id = s.task_id " +
                    "WHERE a.id = ? LIMIT 1", appealId);
            Object pid = row.get("project_id");
            if (pid instanceof Number n) return n.longValue();
        } catch (Exception ignored) { /* skip */ }
        return null;
    }
}
