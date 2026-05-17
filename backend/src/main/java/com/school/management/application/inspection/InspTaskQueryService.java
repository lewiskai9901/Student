package com.school.management.application.inspection;

import com.school.management.infrastructure.inspection.InspectionScopeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 检查任务 — 运营 KPI / 项目模式 配置查询服务.
 *
 * <p>I6 (2026-05-17) 拆出: InspTaskApplicationService 之前 1090 行, 把
 * 与 task lifecycle 无关的 governance KPI / inspection mode CRUD 移到这里, 让主 service
 * 回归"task 状态机"职责.
 *
 * <p>所有 SQL 都走 {@link InspectionScopeHelper} 收窄 org_unit_id 范围, 不绕过数据权限.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspTaskQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final InspectionScopeHelper scopeHelper;

    /**
     * 项目级 governance KPI — 5 类 task type 的运营指标.
     *
     * <p>返回 5 个维度的指标,用于 governance 工作台展示真实业务行为:
     * <ul>
     *   <li>scheduled: 计划完成率 (硬指标, 检查员 KPI)</li>
     *   <li>adHoc: 抽查活跃度 (反映主动发现, 越高越好)</li>
     *   <li>triggered: 事件响应平均时长 (申诉/告警 SLA)</li>
     *   <li>selfCheck: 自查参与度 (受检主体主动性)</li>
     *   <li>crossAudit: 互查覆盖率 (审计独立性)</li>
     * </ul>
     */
    public Map<String, Object> getTaskTypeKpi(Long projectId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // I1: org_unit_id scope (JdbcTemplate 旁路, 不走 DataPermissionInterceptor)
        String scopeClause = scopeHelper.orgScopeClause("org_unit_id");
        String byType = projectId != null
                ? "WHERE project_id = " + projectId.longValue() + " AND deleted = 0" + scopeClause
                : "WHERE deleted = 0" + scopeClause;

        // 1) SCHEDULED 计划完成率
        Map<String, Object> scheduled = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN status IN ('PUBLISHED','REVIEWED') THEN 1 ELSE 0 END) AS completed, " +
                "SUM(CASE WHEN late_submission = 1 THEN 1 ELSE 0 END) AS late " +
                "FROM insp_tasks " + byType + " AND task_type = 'SCHEDULED'");
        long sTotal = num(scheduled.get("total"));
        long sCompleted = num(scheduled.get("completed"));
        long sLate = num(scheduled.get("late"));
        result.put("scheduled", Map.of(
                "total", sTotal,
                "completed", sCompleted,
                "completionRate", sTotal > 0 ? Math.round(sCompleted * 100.0 / sTotal) : 0,
                "lateCount", sLate,
                "onTimeRate", sTotal > 0 ? Math.round((sTotal - sLate) * 100.0 / sTotal) : 0
        ));

        // 2) AD_HOC 抽查活跃度 (近 30 天)
        Map<String, Object> adHoc = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total, " +
                "COUNT(DISTINCT inspector_id) AS uniqueInspectors, " +
                "SUM(CASE WHEN created_at > DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) AS last30d " +
                "FROM insp_tasks " + byType + " AND task_type = 'AD_HOC'");
        result.put("adHoc", Map.of(
                "total", num(adHoc.get("total")),
                "uniqueInspectors", num(adHoc.get("uniqueInspectors")),
                "last30d", num(adHoc.get("last30d"))
        ));

        // 3) TRIGGERED 事件响应 (从 created_at 到 submitted_at 的平均时长)
        Map<String, Object> triggered = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN status IN ('SUBMITTED','UNDER_REVIEW','REVIEWED','PUBLISHED') THEN 1 ELSE 0 END) AS responded, " +
                "AVG(CASE WHEN submitted_at IS NOT NULL " +
                "    THEN TIMESTAMPDIFF(HOUR, created_at, submitted_at) " +
                "    ELSE NULL END) AS avgHours " +
                "FROM insp_tasks " + byType + " AND task_type = 'TRIGGERED'");
        Number avgHoursNum = (Number) triggered.get("avgHours");
        result.put("triggered", Map.of(
                "total", num(triggered.get("total")),
                "responded", num(triggered.get("responded")),
                "avgResponseHours", avgHoursNum == null ? 0 : Math.round(avgHoursNum.doubleValue())
        ));

        // 4) SELF_CHECK 自查参与度
        Map<String, Object> selfCheck = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN status IN ('SUBMITTED','REVIEWED','PUBLISHED') THEN 1 ELSE 0 END) AS submitted, " +
                "COUNT(DISTINCT inspector_id) AS uniqueSubjects " +
                "FROM insp_tasks " + byType + " AND task_type = 'SELF_CHECK'");
        result.put("selfCheck", Map.of(
                "total", num(selfCheck.get("total")),
                "submitted", num(selfCheck.get("submitted")),
                "uniqueSubjects", num(selfCheck.get("uniqueSubjects"))
        ));

        // 5) CROSS_AUDIT 互查覆盖
        Map<String, Object> crossAudit = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total, " +
                "COUNT(DISTINCT inspector_id) AS uniqueAuditors " +
                "FROM insp_tasks " + byType + " AND task_type = 'CROSS_AUDIT'");
        result.put("crossAudit", Map.of(
                "total", num(crossAudit.get("total")),
                "uniqueAuditors", num(crossAudit.get("uniqueAuditors"))
        ));

        return result;
    }

    /** 列出允许抽查的项目 — adHoc UI 选项. */
    public List<Map<String, Object>> listAdHocAllowedProjects() {
        return jdbcTemplate.queryForList(
                "SELECT id, project_name, project_code, inspection_mode, allow_ad_hoc " +
                "FROM insp_projects WHERE deleted = 0 AND allow_ad_hoc = 1" +
                scopeHelper.orgScopeClause("org_unit_id") +
                " ORDER BY id DESC LIMIT 200");
    }

    /** 读项目检查模式配置 — adHoc / selfCheck / quota 等可在 UI 调整. */
    public Map<String, Object> getInspectionMode(Long projectId) {
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT inspection_mode, allow_ad_hoc, allow_self_check, ad_hoc_quota_per_inspector " +
                    "FROM insp_projects WHERE id = ? AND deleted = 0", projectId);
        } catch (Exception e) {
            // 项目不存在或字段缺失, 返回默认值
            Map<String, Object> m = new HashMap<>();
            m.put("inspection_mode", "PLANNED");
            m.put("allow_ad_hoc", 0);
            m.put("allow_self_check", 0);
            m.put("ad_hoc_quota_per_inspector", null);
            return m;
        }
    }

    /** 写项目检查模式配置. */
    @Transactional
    public Map<String, Object> updateInspectionMode(
            Long projectId,
            String inspectionMode,
            Boolean allowAdHoc,
            Boolean allowSelfCheck,
            Integer adHocQuotaPerInspector) {
        if (inspectionMode != null
                && !Set.of("PLANNED", "HYBRID", "SPOT_CHECK", "SELF_AUDIT", "EMERGENCY")
                       .contains(inspectionMode)) {
            throw new IllegalArgumentException("非法的 inspection_mode: " + inspectionMode);
        }
        // PLANNED 模式禁止 allow_ad_hoc
        boolean adHoc = Boolean.TRUE.equals(allowAdHoc) && !"PLANNED".equals(inspectionMode);
        boolean selfCheck = Boolean.TRUE.equals(allowSelfCheck);

        jdbcTemplate.update(
                "UPDATE insp_projects SET inspection_mode = ?, allow_ad_hoc = ?, " +
                "allow_self_check = ?, ad_hoc_quota_per_inspector = ? WHERE id = ?",
                inspectionMode != null ? inspectionMode : "PLANNED",
                adHoc ? 1 : 0,
                selfCheck ? 1 : 0,
                adHocQuotaPerInspector,
                projectId);
        return getInspectionMode(projectId);
    }

    /**
     * 读项目 allow_ad_hoc 字段 — InspProject 实体未冒泡此字段, 直查 DB.
     *
     * <p>I6: 之前 InspTaskApplicationService 用反射 + jdbc fallback 的 readBooleanField helper,
     * InspProject 实际从未有过 allowAdHoc 字段 → 反射总是 NoSuchFieldException 然后 fallback.
     * 清理掉反射, 直接查 DB.
     */
    public boolean isProjectAllowAdHoc(Long projectId) {
        try {
            Integer v = jdbcTemplate.queryForObject(
                    "SELECT allow_ad_hoc FROM insp_projects WHERE id = ? AND deleted = 0",
                    Integer.class, projectId);
            return v != null && v != 0;
        } catch (Exception e) {
            return false;
        }
    }

    private long num(Object v) {
        if (v instanceof Number n) return n.longValue();
        return 0;
    }
}
