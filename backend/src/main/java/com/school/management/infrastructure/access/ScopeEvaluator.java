package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据范围 compose 管线 (统一真相源)。
 *
 * <p>把一个 {@link ScopeSpec} (三正交轴) 编译为一个 {@link ScopeCondition} (SQL + 位置绑定参数)。
 * 这是从旧 {@code DataPermissionInterceptor} 的 {@code buildSingleRoleCondition} /
 * {@code buildMembershipCondition} / {@code buildAccessRelationCondition} /
 * {@code buildPluginDimCondition} / {@code applyTypeFilter} / {@code buildCustomCondition}
 * 重构而来的<b>单一 compose 入口</b>。
 *
 * <p><b>等价保证</b>: 对 ALL/SELF/PRIMARY_ORG(±subtree)/CUSTOM/RELATION(±subtree)/PLUGIN_DIM
 * 这些既有 scope, 本类 emit 的 SQL 与 interceptor 当前各 build* 输出等价 —— T7 瘦壳化的回归基线。
 *
 * <p>管线 (design §3): {@code cond = subjectSelect(orgSet) AND subjectRelFilter(②) AND typeFilter(③)}。
 * 一个 spec → 一个 ScopeCondition。多角色 OR 合并 + ALL 短路留在 interceptor (T7), 不在此处。
 *
 * <p>参数顺序 = SQL 中 {@code ?} 顺序 (orgSet → ② → ③), 名字用 paramOffset 唯一化 (沿用
 * interceptor 的 {@code _dp_*_<offset>} 命名)。
 */
@Slf4j
@Component
public class ScopeEvaluator {

    private final PluginDataScopeRouter pluginDataScopeRouter;

    public ScopeEvaluator(@Lazy PluginDataScopeRouter pluginDataScopeRouter) {
        this.pluginDataScopeRouter = pluginDataScopeRouter;
    }

    /**
     * compose 一个 spec → 一个 ScopeCondition。
     *
     * @param spec        三轴数据范围规格
     * @param meta        资源侧元数据 (别名/org 字段/creator/resourceType/viaMembership/...)
     * @param ctx         当前用户上下文
     * @param tenantId    租户
     * @param paramOffset 参数名唯一化偏移 (多角色时每角色递增)
     */
    public ScopeCondition toSqlCondition(ScopeSpec spec, ResourceScopeMeta meta,
                                         UserContext ctx, Long tenantId, int paramOffset) {
        // 轴① 解析 org id 集合 (单一真相)
        OrgSet orgSet = resolveOrgSet(spec, meta, ctx, tenantId, paramOffset);

        // PLUGIN_DIM 的 deny / inline-id / self-degrade 是终态, 直接由 resolveOrgSet 产出完整 cond
        if (orgSet.terminalCond != null) {
            // 终态仍要叠加类型过滤 (插件维度路径也受闸2/2b约束 — 与 interceptor 一致)
            return typeFilter(orgSet.terminalCond, spec, meta, paramOffset);
        }

        // 轴① 消费: 按资源路径生成 subjectSelect 谓词
        ScopeCondition cond = subjectSelect(orgSet, spec, meta, ctx, tenantId, paramOffset);

        // 轴② subject-relation filter (仅 membership 资源 + include/exclude 非空)
        cond = subjectRelFilter(cond, orgSet, spec, meta, ctx, tenantId, paramOffset);

        // 轴③ type filter
        return typeFilter(cond, spec, meta, paramOffset);
    }

    // ====================================================================
    // 轴① resolveOrgSet —— org id 集合的单一来源
    // ====================================================================

    /**
     * org id 集合的内部表示。把"如何从一个组织列/字段约束出可见行"抽象为一个可复用的谓词模板,
     * subjectSelect 与 subjectRelFilter 共享同一锚定集 (re-derive 时序号 +offset 区分)。
     */
    private static final class OrgSet {
        /** unbounded (ALL): 无 org 约束。 */
        boolean unbounded;
        /** SELF: 落到 creator/self, 不锚定组织。 */
        boolean self;
        /**
         * 给定一个目标列 (如 "t.org_unit_id" 或 "ar.resource_id"), 返回 "col <predicate>"。
         * params 通过 emit() 时回填到传入的 cond。
         */
        OrgPredicate predicate;
        /** PLUGIN_DIM 的终态 cond (inline id / deny / self-degrade) — 非空则短路。 */
        ScopeCondition terminalCond;
    }

    /** 把 org 约束作用到某列上的策略 (column → "column <pred>" + 绑定参数)。 */
    private interface OrgPredicate {
        /** 把 "column <pred>" 写入 cond.sql 片段并 addParam, 返回 SQL 片段。 */
        String emit(String column, ScopeCondition cond, Long tenantId, int paramOffset);
    }

    private OrgSet resolveOrgSet(ScopeSpec spec, ResourceScopeMeta meta,
                                 UserContext ctx, Long tenantId, int paramOffset) {
        OrgSet os = new OrgSet();
        OrgAnchor anchor = spec.getOrgAnchor();
        if (anchor == null) {
            // 未配置锚点 → 安全降级 SELF (与 interceptor "coreScope==null → SELF" 一致)
            os.self = true;
            return os;
        }

        switch (anchor) {
            case ALL:
                os.unbounded = true;
                return os;

            case SELF:
                os.self = true;
                return os;

            case PRIMARY_ORG: {
                Long orgId = ctx.getOrgUnitId();
                String orgPath = ctx.getOrgUnitPath();
                if (spec.isIncludeSubtree() && orgPath != null) {
                    // 端口自 DEPARTMENT_AND_BELOW
                    os.predicate = (column, cond, tid, off) -> {
                        cond.addParam("_dp_tenantId_" + off, tid, Long.class, JdbcType.BIGINT);
                        cond.addParam("_dp_orgPath_" + off, orgPath + "%", String.class, JdbcType.VARCHAR);
                        return column + " IN (SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ?)";
                    };
                } else if (orgId != null) {
                    // 端口自 DEPARTMENT (无子树 / 无 path 退化)
                    os.predicate = (column, cond, tid, off) -> {
                        cond.addParam("_dp_orgId_" + off, orgId, Long.class, JdbcType.BIGINT);
                        return column + " = ?";
                    };
                } else {
                    // 无可锚定 org → 走 self/deny 由 subjectSelect 决定; 这里标记 self=false + no predicate
                    // org-field 路径下无 predicate 会退化为空 cond (interceptor DEPARTMENT orgId==null → 空 sql)
                    os.predicate = null;
                }
                return os;
            }

            case RELATION: {
                // 端口自 MANAGED_ORGS / MANAGED_ORGS_AND_BELOW, 把硬编码 'admin' 换成 anchorParam (?)。
                String relation = spec.getAnchorParam();
                Long userId = ctx.getUserId();
                if (spec.isIncludeSubtree()) {
                    os.predicate = (column, cond, tid, off) -> {
                        cond.addParam("_dp_tenantId_" + off, tid, Long.class, JdbcType.BIGINT);
                        cond.addParam("_dp_mgrUser_" + off, userId, Long.class, JdbcType.BIGINT);
                        cond.addParam("_dp_mgrRel_" + off, relation, String.class, JdbcType.VARCHAR);
                        return column + " IN (" +
                                "SELECT o.id FROM org_units o " +
                                "JOIN org_units mo ON o.tree_path LIKE CONCAT(mo.tree_path, '%') " +
                                "JOIN access_relations mar ON mar.resource_id = mo.id " +
                                "WHERE o.tenant_id = ? AND mar.deleted = 0 AND mar.subject_type = 'user' " +
                                "  AND mar.subject_id = ? AND mar.resource_type = 'org_unit' AND mar.relation = ? " +
                                "  AND (mar.valid_to IS NULL OR mar.valid_to > NOW()))";
                    };
                } else {
                    os.predicate = (column, cond, tid, off) -> {
                        cond.addParam("_dp_mgrUser_" + off, userId, Long.class, JdbcType.BIGINT);
                        cond.addParam("_dp_mgrRel_" + off, relation, String.class, JdbcType.VARCHAR);
                        return column + " IN (" +
                                "SELECT mar.resource_id FROM access_relations mar " +
                                "WHERE mar.deleted = 0 AND mar.subject_type = 'user' AND mar.subject_id = ? " +
                                "  AND mar.resource_type = 'org_unit' AND mar.relation = ? " +
                                "  AND (mar.valid_to IS NULL OR mar.valid_to > NOW()))";
                    };
                }
                return os;
            }

            case CUSTOM_ORG: {
                // 端口自 buildCustomCondition 的 tree_path 子树模式 (id 内联, 安全数值)。
                Set<Long> ids = spec.getCustomOrgIds();
                if (ids == null || ids.isEmpty()) {
                    os.terminalCond = denyAll();
                    return os;
                }
                os.predicate = (column, cond, tid, off) ->
                        emitCustomOrgPredicate(column, ids, spec.isIncludeSubtree(), cond, tid, off);
                return os;
            }

            case PLUGIN_DIM: {
                os.terminalCond = buildPluginDimCondition(spec, meta, ctx, paramOffset);
                return os;
            }

            default:
                os.self = true;
                return os;
        }
    }

    /**
     * CUSTOM_ORG org 谓词。
     * <ul>
     *   <li>includeSubtree=true → 每个 id 用 tree_path 子树展开 (buildCustomCondition withChildren 分支)。</li>
     *   <li>includeSubtree=false → 直接 {@code column IN (id...)} (buildCustomCondition withoutChildren 分支)。</li>
     * </ul>
     * 数值 id 内联 (无注入面)。多个 id OR 连接, 整体加括号。
     */
    private String emitCustomOrgPredicate(String column, Set<Long> ids, boolean subtree,
                                          ScopeCondition cond, Long tenantId, int paramOffset) {
        if (!subtree) {
            String csv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            return "(" + column + " IN (" + csv + "))";
        }
        List<String> parts = new ArrayList<>();
        int idx = paramOffset;
        for (Long id : ids) {
            parts.add(column + " IN (" +
                    "SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE (" +
                    "SELECT CONCAT(tree_path, '%') FROM org_units WHERE id = ?))");
            cond.addParam("_dp_tenantOrg_" + idx, tenantId, Long.class, JdbcType.BIGINT);
            cond.addParam("_dp_childOrg_" + idx, id, Long.class, JdbcType.BIGINT);
            idx++;
        }
        return "(" + String.join(" OR ", parts) + ")";
    }

    /**
     * 端口自 buildPluginDimCondition。
     *   null   → 自降级 SELF (membership: subjectCol = ?; 否则 creatorField = ?)
     *   empty  → 1 = 0
     *   非空    → {alias}id IN (csv)
     */
    private ScopeCondition buildPluginDimCondition(ScopeSpec spec, ResourceScopeMeta meta,
                                                   UserContext ctx, int paramOffset) {
        String alias = meta.aliasPrefix();
        String dimCode = spec.getAnchorParam();
        String resourceType = meta.resourceType();
        ScopeCondition cond = new ScopeCondition();

        List<Long> ids = pluginDataScopeRouter.resolve(dimCode, ctx.getUserId(), resourceType);

        if (ids == null) {
            log.warn("[DataPermission] plugin dim '{}' unavailable, degrading to SELF", dimCode);
            String selfField;
            if (meta.viaMembership()) {
                selfField = meta.membershipSubjectColumn() == null || meta.membershipSubjectColumn().isEmpty()
                        ? "user_id" : meta.membershipSubjectColumn();
            } else {
                selfField = meta.creatorField() == null || meta.creatorField().isEmpty()
                        ? "created_by" : meta.creatorField();
            }
            cond.sql = alias + selfField + " = ?";
            cond.addParam("_dp_pluginSelf_" + paramOffset, ctx.getUserId(), Long.class, JdbcType.BIGINT);
            return cond;
        }
        if (ids.isEmpty()) {
            cond.sql = "1 = 0";
            return cond;
        }
        String csv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        cond.sql = alias + "id IN (" + csv + ")";
        return cond;
    }

    // ====================================================================
    // 轴① 消费 subjectSelect —— 按资源路径把 orgSet 落成谓词
    // ====================================================================

    private ScopeCondition subjectSelect(OrgSet orgSet, ScopeSpec spec, ResourceScopeMeta meta,
                                         UserContext ctx, Long tenantId, int paramOffset) {
        if (orgSet.unbounded) {
            return new ScopeCondition(); // ALL → 空 (无过滤)
        }

        if (meta.viaMembership()) {
            return membershipSelect(orgSet, meta, ctx, tenantId, paramOffset);
        }
        if (meta.hasResourceType()) {
            return accessRelationSelect(orgSet, spec, meta, ctx, tenantId, paramOffset);
        }
        return orgFieldSelect(orgSet, meta, ctx, tenantId, paramOffset);
    }

    /** 端口自 buildSingleRoleCondition 的 org-field switch。 */
    private ScopeCondition orgFieldSelect(OrgSet orgSet, ResourceScopeMeta meta,
                                          UserContext ctx, Long tenantId, int paramOffset) {
        String alias = meta.aliasPrefix();
        String orgField = meta.orgUnitField();
        String creatorField = meta.creatorField();
        ScopeCondition cond = new ScopeCondition();

        if (orgSet.self) {
            cond.sql = alias + creatorField + " = ?";
            cond.addParam("_dp_creatorId_" + paramOffset, ctx.getUserId(), Long.class, JdbcType.BIGINT);
            return cond;
        }
        if (orgSet.predicate == null) {
            // 无可锚定 org (如 PRIMARY_ORG 且 orgId/orgPath 全 null) → 空 sql
            // (interceptor: DEPARTMENT orgId==null → cond.sql 保持空 → 上层放行)
            return cond;
        }
        // CUSTOM_ORG 的 predicate 已自带最外层括号; 其余直接拼。
        cond.sql = orgSet.predicate.emit(alias + orgField, cond, tenantId, paramOffset);
        return cond;
    }

    /**
     * 端口自 buildMembershipCondition。主表行即 member 关系 subject; 按 org 锚定集过滤
     * ar.resource_id。SELF → subjectCol = ?。
     */
    private ScopeCondition membershipSelect(OrgSet orgSet, ResourceScopeMeta meta,
                                            UserContext ctx, Long tenantId, int paramOffset) {
        String alias = meta.aliasPrefix();
        String subjectCol = meta.membershipSubjectColumn() == null || meta.membershipSubjectColumn().isEmpty()
                ? "id" : meta.membershipSubjectColumn();
        ScopeCondition cond = new ScopeCondition();

        if (orgSet.self) {
            cond.sql = alias + subjectCol + " = ?";
            cond.addParam("_dp_self_" + paramOffset, ctx.getUserId(), Long.class, JdbcType.BIGINT);
            return cond;
        }
        if (orgSet.predicate == null) {
            // 无可锚定 org → deny all (避免泄露全表, 与 interceptor membership else 分支一致)
            cond.sql = "1 = 0";
            return cond;
        }

        // ar.tenant_id 必须排在 org 谓词参数之前 (位置绑定) —— interceptor 通过把 tenant param
        // 插到 list 头部实现。这里我们顺序天然正确: 先 add ar.tenant_id, 再 emit org predicate。
        cond.addParam("_dp_memArTenant_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);
        String orgPredicate = orgSet.predicate.emit("ar.resource_id", cond, tenantId, paramOffset);

        cond.sql = alias + subjectCol + " IN ("
                + "SELECT ar.subject_id FROM access_relations ar "
                + "WHERE ar.relation = 'member' AND ar.resource_type = 'org_unit' "
                + "AND ar.subject_type = 'user' AND ar.deleted = 0 AND ar.tenant_id = ? "
                + "AND " + orgPredicate + ")";
        return cond;
    }

    /**
     * 端口自 buildAccessRelationCondition。
     * {@code alias.id IN (SELECT ar.resource_id ... (subject=me) OR (subject_org IN orgSet))}
     * + 对 PRIMARY_ORG(±subtree) 追加 orgField 直过滤的 OR (端口自 DEPARTMENT* 分支)。
     */
    private ScopeCondition accessRelationSelect(OrgSet orgSet, ScopeSpec spec, ResourceScopeMeta meta,
                                                UserContext ctx, Long tenantId, int paramOffset) {
        String alias = meta.aliasPrefix();
        String resourceType = meta.resourceType();
        ScopeCondition cond = new ScopeCondition();

        if (orgSet.self) {
            // SELF on access_relation 资源 → 仅本人 (subject=me) 命中。沿用 access_relation
            // 的 (subject_type='user' AND subject_id=?) 单边。
            cond.sql = alias + "id IN (SELECT ar.resource_id FROM access_relations ar "
                    + "WHERE ar.resource_type = ? AND ar.tenant_id = ? AND ar.deleted = 0 "
                    + "AND ar.subject_type = 'user' AND ar.subject_id = ?)";
            cond.addParam("_dp_resType_" + paramOffset, resourceType, String.class, JdbcType.VARCHAR);
            cond.addParam("_dp_tenantId_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);
            cond.addParam("_dp_userId_" + paramOffset, ctx.getUserId(), Long.class, JdbcType.BIGINT);
            return cond;
        }

        Long orgId = ctx.getOrgUnitId();
        String orgPath = ctx.getOrgUnitPath();

        StringBuilder sb = new StringBuilder();
        sb.append(alias).append("id IN (")
          .append("SELECT ar.resource_id FROM access_relations ar WHERE ar.resource_type = ? AND ar.tenant_id = ? AND ar.deleted = 0 AND (")
          .append("(ar.subject_type = 'user' AND ar.subject_id = ?)");
        cond.addParam("_dp_resType_" + paramOffset, resourceType, String.class, JdbcType.VARCHAR);
        cond.addParam("_dp_tenantId_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);
        cond.addParam("_dp_userId_" + paramOffset, ctx.getUserId(), Long.class, JdbcType.BIGINT);

        boolean subtree = spec.isIncludeSubtree();
        if (orgPath != null && (spec.getOrgAnchor() == OrgAnchor.PRIMARY_ORG ? subtree : true) && usesPathBranch(spec)) {
            sb.append(" OR (ar.subject_type = 'org_unit' AND ar.subject_id IN (")
              .append("SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0))");
            cond.addParam("_dp_tenantId2_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);
            cond.addParam("_dp_orgPath_" + paramOffset, orgPath + "%", String.class, JdbcType.VARCHAR);
        } else if (orgId != null) {
            sb.append(" OR (ar.subject_type = 'org_unit' AND ar.subject_id = ?)");
            cond.addParam("_dp_orgId_" + paramOffset, orgId, Long.class, JdbcType.BIGINT);
        }
        sb.append("))");

        // orgField 直过滤 OR —— 端口自 DEPARTMENT / DEPARTMENT_AND_BELOW (即 PRIMARY_ORG)。
        String orgField = meta.orgUnitField();
        boolean isPrimaryOrg = spec.getOrgAnchor() == OrgAnchor.PRIMARY_ORG;
        if (orgField != null && orgId != null && isPrimaryOrg) {
            sb.insert(0, "(");
            if (subtree && orgPath != null) {
                sb.append(" OR ").append(alias).append(orgField)
                  .append(" IN (SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ?)");
                cond.addParam("_dp_tenantId3_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);
                cond.addParam("_dp_orgPath2_" + paramOffset, orgPath + "%", String.class, JdbcType.VARCHAR);
            } else {
                sb.append(" OR ").append(alias).append(orgField).append(" = ?");
                cond.addParam("_dp_orgDirect_" + paramOffset, orgId, Long.class, JdbcType.BIGINT);
            }
            sb.append(")");
        }

        cond.sql = sb.toString();
        return cond;
    }

    /** access_relation 路径里 org-unit subtree 子查询分支是否启用 (PRIMARY_ORG 之外, 沿用 orgPath 分支)。 */
    private boolean usesPathBranch(ScopeSpec spec) {
        // 端口自 interceptor: orgPath != null 时优先走 subtree 子查询。
        // 对 PRIMARY_ORG 区分 subtree; 对 RELATION/CUSTOM_ORG 等当前 access_relation 路径未覆盖,
        // 保守地按 orgPath 分支 (与 interceptor 的"orgPath != null"无条件分支一致)。
        return true;
    }

    // ====================================================================
    // 轴② subject-relation filter —— 仅 membership 资源 + include/exclude
    // ====================================================================

    private ScopeCondition subjectRelFilter(ScopeCondition cond, OrgSet orgSet, ScopeSpec spec,
                                            ResourceScopeMeta meta, UserContext ctx,
                                            Long tenantId, int paramOffset) {
        // 防御 (§5): 非 membership 资源跳过; include/exclude 均空跳过。
        if (!meta.viaMembership() || (!spec.hasRelInclude() && !spec.hasRelExclude())) {
            return cond;
        }
        // deny / 空 cond 不再叠加 (不放宽 deny)
        if (cond.sql.isEmpty() || "1 = 0".equals(cond.sql)) {
            return cond;
        }

        String alias = meta.aliasPrefix();
        String subjectCol = meta.membershipSubjectColumn() == null || meta.membershipSubjectColumn().isEmpty()
                ? "id" : meta.membershipSubjectColumn();

        StringBuilder sb = new StringBuilder(cond.sql);

        if (spec.hasRelInclude()) {
            String frag = relSubquery(subjectCol, alias, "IN", spec.getSubjectRelInclude(),
                    orgSet, cond, tenantId, paramOffset, "_dp_relInc_");
            sb.append(" AND ").append(frag);
        }
        if (spec.hasRelExclude()) {
            String frag = relSubquery(subjectCol, alias, "NOT IN", spec.getSubjectRelExclude(),
                    orgSet, cond, tenantId, paramOffset, "_dp_relExc_");
            sb.append(" AND ").append(frag);
        }
        cond.sql = sb.toString();
        return cond;
    }

    /**
     * 轴② 关系子查询片段 (NEW — 无 interceptor 等价物)。
     * <pre>
     * alias.subjectCol [NOT] IN (
     *   SELECT ar.subject_id FROM access_relations ar
     *   WHERE ar.subject_type='user' AND ar.relation IN (?...) AND ar.resource_type='org_unit'
     *     AND ar.deleted=0 AND ar.resource_id IN &lt;orgSet&gt;)
     * </pre>
     * orgSet 用与 subjectSelect 相同的锚定集 (re-derive, 参数名加 +offset 区分)。
     */
    private String relSubquery(String subjectCol, String alias, String op, Set<String> relations,
                               OrgSet orgSet, ScopeCondition cond, Long tenantId,
                               int paramOffset, String prefix) {
        String placeholders = relations.stream().map(r -> "?").collect(Collectors.joining(","));
        StringBuilder sub = new StringBuilder();
        sub.append(alias).append(subjectCol).append(" ").append(op).append(" (")
           .append("SELECT ar.subject_id FROM access_relations ar ")
           .append("WHERE ar.subject_type = 'user' AND ar.relation IN (").append(placeholders).append(") ")
           .append("AND ar.resource_type = 'org_unit' AND ar.deleted = 0");

        // 关系码参数 (排在 org 谓词之前 — SQL 中 IN(?) 先于 resource_id IN <orgSet> 出现)
        int i = 0;
        for (String rel : relations) {
            cond.addParam(prefix + paramOffset + "_" + (i++), rel, String.class, JdbcType.VARCHAR);
        }

        // org 锚定: 与 subjectSelect 同集 (re-derive)。unbounded → 无 org 约束; self → 不应到此(membership self 已 deny 叠加跳过)。
        if (!orgSet.unbounded && !orgSet.self && orgSet.predicate != null) {
            String orgFrag = orgSet.predicate.emit("ar.resource_id", cond, tenantId, paramOffset + 1000);
            sub.append(" AND ").append(orgFrag);
        }
        sub.append(")");
        return sub.toString();
    }

    // ====================================================================
    // 轴③ type filter —— 端口自 applyTypeFilter
    // ====================================================================

    private ScopeCondition typeFilter(ScopeCondition cond, ScopeSpec spec,
                                      ResourceScopeMeta meta, int paramOffset) {
        if (!spec.hasTypeFilter() || !meta.hasTypeField()) {
            return cond;
        }
        // 空 / deny → 原样返回 (不放宽)
        if (cond.sql.isEmpty() && !spec.isOrgUnbounded()) {
            // org 解析失败的退化空 cond (非 ALL) → 不补类型 (与 interceptor cond.sql.isEmpty() 分支一致)
            return cond;
        }
        if ("1 = 0".equals(cond.sql)) {
            return cond;
        }

        String alias = meta.aliasPrefix();
        String typeField = meta.typeField();
        Set<String> typeValues = spec.getTypeFilter();
        String placeholders = typeValues.stream().map(v -> "?").collect(Collectors.joining(","));
        String typeSql = alias + typeField + " IN (" + placeholders + ")";

        boolean orgUnbounded = cond.sql.isEmpty(); // ALL → 仅类型谓词
        cond.sql = orgUnbounded ? typeSql : "(" + cond.sql + " AND " + typeSql + ")";

        int i = 0;
        for (String code : typeValues) {
            cond.addParam("_dp_type_" + paramOffset + "_" + (i++), code, String.class, JdbcType.VARCHAR);
        }
        return cond;
    }

    private ScopeCondition denyAll() {
        ScopeCondition c = new ScopeCondition();
        c.sql = "1 = 0";
        return c;
    }
}
