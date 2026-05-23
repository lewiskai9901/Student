package com.school.management.application.inspection;

import com.school.management.application.inspection.dto.PeopleWorkbenchView;
import com.school.management.application.inspection.dto.PeopleWorkbenchView.*;
import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.InspectorRole;
import com.school.management.domain.inspection.model.execution.ProjectInspector;
import com.school.management.domain.inspection.model.execution.TaskStatus;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.ProjectInspectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase B - 人员工作台聚合查询服务.
 *
 * <p>读侧专用 — 不修改任何业务数据, 仅在写路径之外做高内聚的视图组合.
 * 设计原则:
 * <ul>
 *   <li>"先查全后过滤" — 一次拉取项目所有 inspectors + 全部 tasks, 内存计算 stats / 分组,
 *       避免分人 N 次查询. 项目通常 < 100 任务 + < 30 人, 内存计算开销可忽略.</li>
 *   <li>每个 PersonRow 同时包含折叠态 stats 和展开后 tasks — 减少前端二次请求.</li>
 *   <li>ensureProjectHasLead 兜底 — 进入视图时如果项目无 LEAD, 自动升级首个 inspector.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeopleWorkbenchQueryService {

    private final InspProjectRepository projectRepository;
    private final ProjectInspectorRepository inspectorRepository;
    private final InspTaskRepository taskRepository;
    private final InspProjectAuthorizationGuard authGuard;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public PeopleWorkbenchView getWorkbench(Long projectId) {
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        // 兜底: 项目无 LEAD 时自动升级 (write path, 写在 readOnly tx 之外没问题
        // 因 ensureProjectHasLead 内部已 transactional)
        authGuard.ensureProjectHasLead(projectId);

        List<ProjectInspector> inspectors = inspectorRepository.findByProjectId(projectId);
        List<InspTask> tasks = taskRepository.findByProjectId(projectId);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6); // 本周 = 最近 7 天

        // 同一用户可有多 (user, role) 行 — 按 userId 聚合: 取首个 user_name + 合并 roles
        Map<Long, List<ProjectInspector>> byUserId = inspectors.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()))
                .collect(Collectors.groupingBy(ProjectInspector::getUserId, LinkedHashMap::new, Collectors.toList()));

        // 部门名映射 (jdbc 直查, 避免引依赖)
        Map<Long, String> orgUnitNames = lookupUserOrgUnitNames(byUserId.keySet());

        // 待分配任务 (PENDING / 无 inspectorId)
        List<TaskRow> pendingAssign = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING && t.getInspectorId() == null)
                .map(this::toTaskRow)
                .collect(Collectors.toList());

        // Per-person rows
        List<PersonRow> people = byUserId.entrySet().stream()
                .map(e -> buildPersonRow(project, e.getKey(), e.getValue(), tasks, orgUnitNames, today, weekStart))
                .sorted(comparePeopleByUrgency())
                .collect(Collectors.toList());

        // Lead first (即便没待办)
        people = pinLeadsFirst(people);

        // Summary
        Summary summary = buildSummary(inspectors, tasks, pendingAssign, today, people);

        return PeopleWorkbenchView.builder()
                .projectId(projectId)
                .projectCode(project.getProjectCode())
                .projectName(project.getProjectName())
                .summary(summary)
                .people(people)
                .pendingAssignTasks(pendingAssign)
                .build();
    }

    private PersonRow buildPersonRow(InspProject project, Long userId, List<ProjectInspector> rows,
                                      List<InspTask> allTasks, Map<Long, String> orgUnitNames,
                                      LocalDate today, LocalDate weekStart) {
        ProjectInspector first = rows.get(0);
        List<String> roles = rows.stream()
                .map(r -> r.getRole().name())
                .distinct()
                .collect(Collectors.toList());
        boolean isCreator = Objects.equals(project.getCreatedBy(), userId);

        // 该人作为 inspector 的任务
        List<InspTask> asInspector = allTasks.stream()
                .filter(t -> Objects.equals(t.getInspectorId(), userId))
                .collect(Collectors.toList());
        // 该人作为 reviewer 的任务
        List<InspTask> asReviewer = allTasks.stream()
                .filter(t -> Objects.equals(t.getReviewerId(), userId))
                .collect(Collectors.toList());

        // Stats
        int weekAssigned = (int) asInspector.stream()
                .filter(t -> t.getTaskDate() != null && !t.getTaskDate().isBefore(weekStart))
                .count();
        int weekCompleted = (int) asInspector.stream()
                .filter(t -> t.getTaskDate() != null && !t.getTaskDate().isBefore(weekStart))
                .filter(t -> t.getStatus() == TaskStatus.PUBLISHED || t.getStatus() == TaskStatus.REVIEWED)
                .count();
        int inProgress = (int) asInspector.stream()
                .filter(t -> isInProgressStatus(t.getStatus()))
                .count();
        int pendingReview = (int) asReviewer.stream()
                .filter(t -> t.getStatus() == TaskStatus.UNDER_REVIEW)
                .count();
        int overdue = (int) asInspector.stream()
                .filter(t -> isInProgressStatus(t.getStatus()))
                .filter(t -> t.getTaskDate() != null && t.getTaskDate().isBefore(today))
                .count();
        int totalAssigned = asInspector.size();
        int totalCompleted = (int) asInspector.stream()
                .filter(t -> t.getStatus() == TaskStatus.PUBLISHED || t.getStatus() == TaskStatus.REVIEWED)
                .count();

        // 候选 (v1 简化: 该人是 INSPECTOR/LEAD 则可作为待分配的候选)
        boolean canBeAssigned = roles.contains("INSPECTOR") || roles.contains("LEAD");
        int pendingAssignCandidates = 0;
        if (canBeAssigned) {
            pendingAssignCandidates = (int) allTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.PENDING && t.getInspectorId() == null)
                    .count();
        }

        PersonStats stats = PersonStats.builder()
                .weekAssigned(weekAssigned)
                .weekCompleted(weekCompleted)
                .pendingAssignCandidates(pendingAssignCandidates)
                .inProgress(inProgress)
                .pendingReview(pendingReview)
                .overdue(overdue)
                .totalAssigned(totalAssigned)
                .totalCompleted(totalCompleted)
                .build();

        // Tasks 分 4 段 (实际填 3 段, pendingAssign 在共享池)
        PersonTasks tasksGroup = PersonTasks.builder()
                .inProgress(asInspector.stream()
                        .filter(t -> isInProgressStatus(t.getStatus()))
                        .filter(t -> !(t.getTaskDate() != null && t.getTaskDate().isBefore(today)))
                        .map(this::toTaskRow)
                        .collect(Collectors.toList()))
                .pendingReview(asReviewer.stream()
                        .filter(t -> t.getStatus() == TaskStatus.UNDER_REVIEW)
                        .map(this::toTaskRow)
                        .collect(Collectors.toList()))
                .overdue(asInspector.stream()
                        .filter(t -> isInProgressStatus(t.getStatus()))
                        .filter(t -> t.getTaskDate() != null && t.getTaskDate().isBefore(today))
                        .map(this::toTaskRow)
                        .collect(Collectors.toList()))
                .build();

        return PersonRow.builder()
                .userId(userId)
                .userName(first.getUserName())
                .orgUnitName(orgUnitNames.get(userId))
                .roles(roles)
                .isActive(Boolean.TRUE.equals(first.getIsActive()))
                .isCreator(isCreator)
                .stats(stats)
                .tasks(tasksGroup)
                .build();
    }

    private Summary buildSummary(List<ProjectInspector> inspectors, List<InspTask> tasks,
                                  List<TaskRow> pendingAssign, LocalDate today, List<PersonRow> people) {
        long inspectorCount = inspectors.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()) && i.getRole() == InspectorRole.INSPECTOR)
                .map(ProjectInspector::getUserId)
                .distinct().count();
        long reviewerCount = inspectors.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()) && i.getRole() == InspectorRole.REVIEWER)
                .map(ProjectInspector::getUserId)
                .distinct().count();
        long leadCount = inspectors.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()) && i.getRole() == InspectorRole.LEAD)
                .map(ProjectInspector::getUserId)
                .distinct().count();
        long totalPeople = inspectors.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()))
                .map(ProjectInspector::getUserId)
                .distinct().count();

        int pendingReview = (int) tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.UNDER_REVIEW || t.getStatus() == TaskStatus.SUBMITTED)
                .count();
        int inProgress = (int) tasks.stream()
                .filter(t -> isInProgressStatus(t.getStatus()))
                .count();
        int overdue = (int) tasks.stream()
                .filter(t -> isInProgressStatus(t.getStatus()))
                .filter(t -> t.getTaskDate() != null && t.getTaskDate().isBefore(today))
                .count();

        String leadName = people.stream()
                .filter(p -> p.getRoles().contains("LEAD"))
                .map(PersonRow::getUserName)
                .findFirst()
                .orElse(null);

        return Summary.builder()
                .totalPeople((int) totalPeople)
                .totalInspectors((int) inspectorCount)
                .totalReviewers((int) reviewerCount)
                .totalLeads((int) leadCount)
                .pendingAssignCount(pendingAssign.size())
                .pendingReviewCount(pendingReview)
                .inProgressCount(inProgress)
                .overdueCount(overdue)
                .leadName(leadName)
                .build();
    }

    private TaskRow toTaskRow(InspTask t) {
        LocalDate today = LocalDate.now();
        int daysOverdue = 0;
        if (isInProgressStatus(t.getStatus()) && t.getTaskDate() != null && t.getTaskDate().isBefore(today)) {
            daysOverdue = (int) java.time.temporal.ChronoUnit.DAYS.between(t.getTaskDate(), today);
        }
        return TaskRow.builder()
                .taskId(t.getId())
                .taskCode(t.getTaskCode())
                .taskDate(t.getTaskDate())
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .inspectorId(t.getInspectorId())
                .inspectorName(t.getInspectorName())
                .reviewerId(t.getReviewerId())
                .reviewerName(t.getReviewerName())
                .totalTargets(t.getTotalTargets())
                .completedTargets(t.getCompletedTargets())
                .submittedAt(t.getSubmittedAt())
                .daysOverdue(daysOverdue)
                .build();
    }

    private boolean isInProgressStatus(TaskStatus s) {
        return s == TaskStatus.CLAIMED || s == TaskStatus.IN_PROGRESS || s == TaskStatus.SUBMITTED;
    }

    /** 排序: 逾期 desc → 待审核 desc → 待分配候选 desc → 进行中 desc → totalAssigned desc */
    private Comparator<PersonRow> comparePeopleByUrgency() {
        return Comparator.<PersonRow, Integer>comparing(p -> -p.getStats().getOverdue())
                .thenComparing(p -> -p.getStats().getPendingReview())
                .thenComparing(p -> -p.getStats().getInProgress())
                .thenComparing(p -> -p.getStats().getTotalAssigned());
    }

    /** LEAD 置顶 — 即使没待办也放最前 (项目负责人始终可见) */
    private List<PersonRow> pinLeadsFirst(List<PersonRow> sorted) {
        List<PersonRow> leads = sorted.stream()
                .filter(p -> p.getRoles().contains("LEAD"))
                .collect(Collectors.toList());
        List<PersonRow> others = sorted.stream()
                .filter(p -> !p.getRoles().contains("LEAD"))
                .collect(Collectors.toList());
        List<PersonRow> result = new ArrayList<>(leads.size() + others.size());
        result.addAll(leads);
        result.addAll(others);
        return result;
    }

    /** 一次查 users + org_units, 避免逐人 N+1. */
    private Map<Long, String> lookupUserOrgUnitNames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        try {
            String inClause = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT u.id AS user_id, o.org_name AS org_name " +
                    "FROM users u LEFT JOIN org_units o ON o.id = u.org_unit_id AND o.deleted = 0 " +
                    "WHERE u.deleted = 0 AND u.id IN (" + inClause + ")");
            Map<Long, String> map = new HashMap<>();
            for (Map<String, Object> r : rows) {
                Object uid = r.get("user_id");
                Object name = r.get("org_name");
                if (uid instanceof Number n) {
                    map.put(n.longValue(), name != null ? name.toString() : null);
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("查询用户部门信息失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
