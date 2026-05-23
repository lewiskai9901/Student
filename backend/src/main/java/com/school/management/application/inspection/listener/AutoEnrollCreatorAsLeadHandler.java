package com.school.management.application.inspection.listener;

import com.school.management.application.inspection.InspProjectApplicationService;
import com.school.management.domain.inspection.event.ProjectCreatedEvent;
import com.school.management.domain.inspection.model.execution.InspectorRole;
import com.school.management.domain.inspection.model.execution.ProjectInspector;
import com.school.management.domain.inspection.repository.ProjectInspectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 项目创建 → 自动把 createdBy 注册为项目负责人 (LEAD).
 *
 * <p>2026-05-23 设计目标: 把装饰性的 LEAD 枚举改造为真实的"项目负责人":
 * <ul>
 *   <li>创建者自动获得 LEAD 身份, 出现在人员名单顶部 — 不再"创建者隐身"</li>
 *   <li>之后修改项目设置 / 审核任务 都通过 InspProjectAuthorizationGuard 校验是否 LEAD 或 admin</li>
 * </ul>
 *
 * <p>设计要点 (沿 TriggeredTaskAutoCreateListener 模式):
 * <ul>
 *   <li>{@code @TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)} —
 *       上游事务提交后才写, 避免上游回滚导致孤儿 LEAD; fallbackExecution 保证测试场景无事务也能跑</li>
 *   <li>{@code @Async} — 异步处理, 不阻塞主流程</li>
 *   <li>幂等: 写入前查 (project, user, LEAD) 是否已存在, 已存在则跳过</li>
 *   <li>失败安全降级: 任何异常吞掉 + log warn, 避免影响业务主链路;
 *       后续可由 ensureProjectHasLead 补偿 (Phase B people-workbench 入口校验)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoEnrollCreatorAsLeadHandler {

    private final ProjectInspectorRepository inspectorRepository;
    private final JdbcTemplate jdbcTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Async
    public void onProjectCreated(ProjectCreatedEvent event) {
        Long projectId = event.getProjectId();
        Long userId = event.getCreatedBy();
        if (projectId == null || userId == null) {
            log.debug("[LEAD-Auto] 跳过自动绑定: projectId={} or createdBy={} 为空", projectId, userId);
            return;
        }

        try {
            // 幂等: 已存在则跳过
            if (inspectorRepository.findOneByProjectUserRole(projectId, userId, InspectorRole.LEAD).isPresent()) {
                log.debug("[LEAD-Auto] 项目 {} 的 LEAD {} 已存在, 跳过", projectId, userId);
                return;
            }

            String userName = lookupUserName(userId);
            ProjectInspector lead = ProjectInspector.create(projectId, userId, userName, InspectorRole.LEAD);
            inspectorRepository.save(lead);
            log.info("[LEAD-Auto] 项目 {} 创建后自动绑定负责人: userId={}, userName={}",
                    projectId, userId, userName);
        } catch (Exception e) {
            // 不抛: 主链路已 COMMIT, 这里失败只是 LEAD 暂未绑定, 由后续补偿守护
            log.warn("[LEAD-Auto] 项目 {} 自动绑定 LEAD 失败 (userId={}): {}",
                    projectId, userId, e.getMessage());
        }
    }

    /** 查 users 表拿 username (容错: 查不到返回 null, ProjectInspector 允许 null userName). */
    private String lookupUserName(Long userId) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT username, real_name FROM users WHERE id = ? AND deleted = 0 LIMIT 1", userId);
            Object realName = row.get("real_name");
            if (realName != null && !realName.toString().isBlank()) return realName.toString();
            Object username = row.get("username");
            return username != null ? username.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
