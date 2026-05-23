package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.InspectorRole;
import com.school.management.domain.inspection.model.execution.ProjectInspector;
import com.school.management.domain.inspection.repository.ProjectInspectorRepository;
import com.school.management.infrastructure.access.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 检查项目授权守护 — 2026-05-23 把装饰性 InspectorRole 改造成真授权:
 *
 * <ul>
 *   <li>{@link #assertCanEditSettings} — 修改项目设置 (project name/policy/scope/...)
 *       要求 LEAD 或 admin</li>
 *   <li>{@link #assertCanReview} — 审核任务/申诉 要求 REVIEWER 或 LEAD 或 admin</li>
 *   <li>{@link #isLead} / {@link #isReviewer} / {@link #isInspector} — 矩阵视图 toggle 读取用</li>
 * </ul>
 *
 * <p>admin 直通 — 通过 UserContextHolder.isSuperAdmin() 判定, 避免锁死.
 *
 * <p>userId 为 null (无登录态, 例如调度/批处理) 直接放行, 由调用方自己保证安全.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspProjectAuthorizationGuard {

    private final ProjectInspectorRepository inspectorRepository;

    /**
     * 校验当前用户能修改项目设置 (改名 / 改运营配置 / 改策略 / 改范围 / 调度组 / 评分方案绑定 ...).
     *
     * @throws InspProjectAuthorizationException 非 admin 且非 LEAD
     */
    public void assertCanEditSettings(Long projectId, Long userId) {
        if (userId == null) return; // 无登录态 (例如系统调度), 不做检查
        if (UserContextHolder.isSuperAdmin()) return; // admin 直通
        if (isLead(projectId, userId)) return;
        log.warn("[Auth] 用户 {} 无权修改项目 {} 设置 (非 LEAD)", userId, projectId);
        throw new InspProjectAuthorizationException(
                "无权修改本项目, 仅项目负责人 (LEAD) 或系统管理员可操作.");
    }

    /**
     * 校验当前用户能审核任务/申诉.
     * 要求是 REVIEWER 或 LEAD (LEAD 可代审, 避免审核员缺位时阻塞业务).
     *
     * @throws InspProjectAuthorizationException 非 admin 且非 REVIEWER/LEAD
     */
    public void assertCanReview(Long projectId, Long userId) {
        if (userId == null) return;
        if (UserContextHolder.isSuperAdmin()) return;
        if (isReviewer(projectId, userId) || isLead(projectId, userId)) return;
        log.warn("[Auth] 用户 {} 无权审核项目 {} (非 REVIEWER/LEAD)", userId, projectId);
        throw new InspProjectAuthorizationException(
                "无权审核本项目任务, 仅审核员 / 项目负责人 / 系统管理员可操作.");
    }

    public boolean isLead(Long projectId, Long userId) {
        return inspectorRepository.findOneByProjectUserRole(projectId, userId, InspectorRole.LEAD)
                .map(ProjectInspector::getIsActive)
                .orElse(false);
    }

    public boolean isReviewer(Long projectId, Long userId) {
        return inspectorRepository.findOneByProjectUserRole(projectId, userId, InspectorRole.REVIEWER)
                .map(ProjectInspector::getIsActive)
                .orElse(false);
    }

    public boolean isInspector(Long projectId, Long userId) {
        return inspectorRepository.findOneByProjectUserRole(projectId, userId, InspectorRole.INSPECTOR)
                .map(ProjectInspector::getIsActive)
                .orElse(false);
    }

    /**
     * 补偿守护 — 进入团队视图时调用, 如果项目无 LEAD (上游事件失败/老数据),
     * 自动选择最早的 INSPECTOR/REVIEWER 升级为 LEAD, 兜底保证不变量.
     *
     * <p>失败安全: 完全无人 (空列表) 不强升, 由前端提示管理员补加 LEAD.
     */
    public void ensureProjectHasLead(Long projectId) {
        int leadCount = inspectorRepository.countActiveByProjectIdAndRole(projectId, InspectorRole.LEAD);
        if (leadCount > 0) return;
        List<ProjectInspector> all = inspectorRepository.findByProjectId(projectId);
        if (all.isEmpty()) {
            log.warn("[Auth] 项目 {} 无任何成员, 无法自动补 LEAD", projectId);
            return;
        }
        // 选 id 最小 = 最早加入的 active inspector
        ProjectInspector first = all.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()))
                .min((a, b) -> Long.compare(a.getId(), b.getId()))
                .orElse(null);
        if (first == null) return;
        ProjectInspector promoted = ProjectInspector.create(
                projectId, first.getUserId(), first.getUserName(), InspectorRole.LEAD);
        inspectorRepository.save(promoted);
        log.info("[Auth] 项目 {} 无 LEAD, 自动升级用户 {} 为负责人", projectId, first.getUserId());
    }

    /**
     * 项目级授权异常 — 401/403 由 Controller 异常处理器映射.
     */
    public static class InspProjectAuthorizationException extends RuntimeException {
        public InspProjectAuthorizationException(String message) {
            super(message);
        }
    }
}
