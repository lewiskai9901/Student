package com.school.management.infrastructure.inspection;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.school.management.infrastructure.access.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus MetaObjectHandler — 自动填充 inspection_* 表的 org_unit_id 字段.
 *
 * <p>填充优先级:
 * <ol>
 *   <li>业务层显式 set (override 通道) — 已有非 null 值, 保留</li>
 *   <li>{@link InspectionUpstreamRouter} 反查上游 PO (task→project, evidence→submission, ...)</li>
 *   <li>{@link UserContextHolder#getOrgUnitId()} 兜底 (创建者所在组织)</li>
 *   <li>都失败 — 静默 (生产环境通过 metric/log 监控 fallback 漏率)</li>
 * </ol>
 *
 * <p>设计原则: org_unit_id 是数据权限基础设施字段, 等同于 tenant_id / created_at / deleted —
 * 由拦截器自动填充, 业务模型零感知. 对齐 Hibernate Envers / Spring Auditing 同等地位.
 *
 * <p>UPDATE 阶段不动 — 防止 UPDATE 误改 org_unit_id 越权.
 */
@Component
public class InspectionDataPermissionFiller implements MetaObjectHandler {

    private static final Logger log = LoggerFactory.getLogger(InspectionDataPermissionFiller.class);

    private static final String ORG_UNIT_FIELD = "orgUnitId";

    private final InspectionUpstreamRouter router;

    public InspectionDataPermissionFiller(InspectionUpstreamRouter router) {
        this.router = router;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        Object target = metaObject.getOriginalObject();
        if (target == null) return;
        // 仅处理已注册的 inspection PO 类型, 避免污染其他模块的 PO
        if (!router.registeredTypes().contains(target.getClass())) return;
        // PO 必须有 orgUnitId 字段 (ArchUnit 守护)
        if (!metaObject.hasGetter(ORG_UNIT_FIELD)) return;

        // 业务层已显式赋值 — 保留, 不覆盖
        Object existing = metaObject.getValue(ORG_UNIT_FIELD);
        if (existing != null) return;

        // 1. 反查上游
        Long resolved = router.resolve(target);
        if (resolved != null) {
            metaObject.setValue(ORG_UNIT_FIELD, resolved);
            return;
        }

        // 2. SecurityContext 兜底 (创建者所在组织)
        Long fallback = UserContextHolder.getOrgUnitId();
        if (fallback != null) {
            metaObject.setValue(ORG_UNIT_FIELD, fallback);
            return;
        }

        // 3. 都失败 — 静默 + log (生产环境通过 metric 兜底)
        if (log.isWarnEnabled()) {
            log.warn("orgUnitId fill missed for {} — both upstream lookup and SecurityContext returned null. " +
                            "Row will INSERT with org_unit_id=NULL and bypass DataPermissionInterceptor filter.",
                    target.getClass().getSimpleName());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 永远不在 UPDATE 阶段填充 orgUnitId — 防止越权改边界
    }
}
