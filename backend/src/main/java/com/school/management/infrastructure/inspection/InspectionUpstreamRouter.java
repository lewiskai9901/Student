package com.school.management.infrastructure.inspection;

import com.school.management.infrastructure.persistence.inspection.analytics.AlertPO;
import com.school.management.infrastructure.persistence.inspection.analytics.AlertRulePO;
import com.school.management.infrastructure.persistence.inspection.analytics.CorrectiveSummaryPO;
import com.school.management.infrastructure.persistence.inspection.analytics.InspectorSummaryPO;
import com.school.management.infrastructure.persistence.inspection.analytics.ItemFrequencySummaryPO;
import com.school.management.infrastructure.persistence.inspection.corrective.CorrectiveCaseMapper;
import com.school.management.infrastructure.persistence.inspection.corrective.CorrectiveCasePO;
import com.school.management.infrastructure.persistence.inspection.corrective.CorrectiveSubtaskPO;
import com.school.management.infrastructure.persistence.inspection.execution.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 中央路由表 — 把 inspection PO 反查到上游 PO 的 orgUnitId.
 *
 * <p>由 {@link InspectionDataPermissionFiller} 在 INSERT 阶段调用, 实现
 * "数据权限边界自动从父聚合继承". org_unit_id 对齐 tenant_id / created_at /
 * deleted 的横切关注点定位 — 业务模型零感知.
 *
 * <p>注册的 PO 类即"已认证写入策略" — {@code InspectionWriteMustSetOrgUnitIdTest}
 * (ArchUnit) 检查每个 inspection PO 要么被注册, 要么源代码出现 setOrgUnitId(...)
 * 显式调用 (用于源头 InspProject 这类无法继承的场景).
 *
 * <p>新加 inspection PO 需要数据权限过滤? 在 {@link #routes} 里添加一行映射.
 */
@Component
public class InspectionUpstreamRouter {

    private final Map<Class<?>, Function<Object, Long>> routes = new LinkedHashMap<>();

    public InspectionUpstreamRouter(InspProjectMapper projectMapper,
                                    InspSubmissionMapper submissionMapper,
                                    CorrectiveCaseMapper caseMapper) {
        // 源头: 项目无上游, 由 SecurityContext 兜底 (handler 内处理)
        register(InspProjectPO.class, po -> null);

        // 项目下游: 全部反查 project.orgUnitId
        register(InspTaskPO.class, po -> {
            InspTaskPO t = (InspTaskPO) po;
            return resolveProjectOrgUnit(projectMapper, t.getProjectId());
        });
        register(ProjectInspectorPO.class, po -> {
            ProjectInspectorPO i = (ProjectInspectorPO) po;
            return resolveProjectOrgUnit(projectMapper, i.getProjectId());
        });
        register(AlertRulePO.class, po -> {
            AlertRulePO r = (AlertRulePO) po;
            return resolveProjectOrgUnit(projectMapper, r.getProjectId());
        });

        // submission 下游: 反查 submission.orgUnitId
        register(InspEvidencePO.class, po -> {
            InspEvidencePO e = (InspEvidencePO) po;
            return resolveSubmissionOrgUnit(submissionMapper, e.getSubmissionId());
        });
        register(SubmissionDetailPO.class, po -> {
            SubmissionDetailPO d = (SubmissionDetailPO) po;
            return resolveSubmissionOrgUnit(submissionMapper, d.getSubmissionId());
        });
        register(ViolationRecordPO.class, po -> {
            ViolationRecordPO v = (ViolationRecordPO) po;
            return resolveSubmissionOrgUnit(submissionMapper, v.getSubmissionId());
        });

        // corrective case 下游
        register(CorrectiveSubtaskPO.class, po -> {
            CorrectiveSubtaskPO s = (CorrectiveSubtaskPO) po;
            if (s.getCaseId() == null) return null;
            CorrectiveCasePO c = caseMapper.selectById(s.getCaseId());
            return c == null ? null : c.getOrgUnitId();
        });

        // 告警: target_type=ORG_UNIT 时, target_id 即为 orgUnitId; 否则无法继承
        register(AlertPO.class, po -> {
            AlertPO a = (AlertPO) po;
            if ("ORG_UNIT".equals(a.getTargetType()) || "ORG".equals(a.getTargetType())) {
                return a.getTargetId();
            }
            return null;
        });

        // V20260501_1 新建的 3 张 summary 表 — projection 时按 project 维度聚合, 反查 project.orgUnitId
        register(InspectorSummaryPO.class, po -> {
            InspectorSummaryPO s = (InspectorSummaryPO) po;
            return resolveProjectOrgUnit(projectMapper, s.getProjectId());
        });
        register(CorrectiveSummaryPO.class, po -> {
            CorrectiveSummaryPO s = (CorrectiveSummaryPO) po;
            return resolveProjectOrgUnit(projectMapper, s.getProjectId());
        });
        register(ItemFrequencySummaryPO.class, po -> {
            ItemFrequencySummaryPO s = (ItemFrequencySummaryPO) po;
            return resolveProjectOrgUnit(projectMapper, s.getProjectId());
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void register(Class<T> type, Function<T, Long> route) {
        routes.put(type, (Function<Object, Long>) route);
    }

    /**
     * 反查目标 PO 的 orgUnitId. 若 PO 类型未注册, 返回 null (handler 走 SecurityContext 兜底).
     */
    public Long resolve(Object po) {
        if (po == null) return null;
        Function<Object, Long> route = routes.get(po.getClass());
        return route == null ? null : route.apply(po);
    }

    /** 已注册的 PO 类集合 — 给 ArchUnit 守护测试做覆盖判定. */
    public Set<Class<?>> registeredTypes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(routes.keySet()));
    }

    private static Long resolveProjectOrgUnit(InspProjectMapper mapper, Long projectId) {
        if (projectId == null) return null;
        InspProjectPO p = mapper.selectById(projectId);
        return p == null ? null : p.getOrgUnitId();
    }

    private static Long resolveSubmissionOrgUnit(InspSubmissionMapper mapper, Long submissionId) {
        if (submissionId == null) return null;
        InspSubmissionPO s = mapper.selectById(submissionId);
        return s == null ? null : s.getOrgUnitId();
    }
}
