package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.StorageKind;
import com.school.management.domain.access.model.SubjectScope;
import com.school.management.domain.access.model.valueobject.RelationGrant;
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
 *
 * <p>subjectSelect 三路曾含 access_relation 路径 (resourceType 非空); R4 后清理已删 —— resourceType
 * 全无来源 (经核实) → 该路径 production 不可达, 是 registry 化 (viaMembership) 前的 legacy。现仅
 * membership / org-field 两路 + RECORD_RELATION (composeGrant 分流)。
 */
@Slf4j
@Component
public class ScopeEvaluator {

    /** deny-all 谓词常量 (emit + guard 检查统一用, 防字面量漂移)。 */
    private static final String DENY = "1 = 0";

    private final PluginDataScopeRouter pluginDataScopeRouter;
    private final ResourceRelationRegistry resourceRelationRegistry;
    private final RecordRelationResolverRouter recordRelationResolverRouter;
    private final ChainCompiler chainCompiler;

    public ScopeEvaluator(@Lazy PluginDataScopeRouter pluginDataScopeRouter,
                          ResourceRelationRegistry resourceRelationRegistry,
                          @Lazy RecordRelationResolverRouter recordRelationResolverRouter,
                          ChainCompiler chainCompiler) {
        this.pluginDataScopeRouter = pluginDataScopeRouter;
        this.resourceRelationRegistry = resourceRelationRegistry;
        this.recordRelationResolverRouter = recordRelationResolverRouter;
        this.chainCompiler = chainCompiler;
    }

    /**
     * compose 一个 spec → 一个 ScopeCondition (R3: 多 grant OR 入口)。
     *
     * <p>可见性来源: {@code relation_grants} (非空) 各 grant 子条件 <b>OR</b> 叠加 (多锚点);
     * 为空时由轴① ({@code orgAnchor}) bridge 派生<b>单</b> grant (R3a 过渡, 与 M1 逐字节等价)。
     * 每条 grant 转等价单锚点 sub-spec, 复用 {@link #composeAnchorCondition} (= 原单锚点 compose,
     * 内部零改) 出子条件。
     *
     * <p>单 grant → 直接复用单锚点路径 (anchor→grant→sub-spec→anchor 往返恒等 ⇒ 字节等价);
     * 多 grant → 各子条件 OR, 每 grant paramOffset 加大间隔避免参数名碰撞; 任一 grant unbounded(空)
     * ⇒ 并集放行; 全 deny ⇒ deny。typeFilter 每-grant 应用, 因 (A∧T)∨(B∧T)=(A∨B)∧T 逻辑等价。
     */
    public ScopeCondition toSqlCondition(ScopeSpec spec, ResourceScopeMeta meta,
                                         UserContext ctx, Long effectiveOrgId, String effectiveOrgPath,
                                         Long tenantId, int paramOffset) {
        List<RelationGrant> grants = grantsOf(spec, meta);

        if (grants.size() == 1) {
            return composeGrant(grants.get(0), spec, meta, ctx,
                    effectiveOrgId, effectiveOrgPath, tenantId, paramOffset);
        }

        List<String> parts = new ArrayList<>();
        ScopeCondition combined = new ScopeCondition();
        int i = 0;
        for (RelationGrant g : grants) {
            ScopeCondition sub = composeGrant(g, spec, meta, ctx,
                    effectiveOrgId, effectiveOrgPath, tenantId, paramOffset + i * 10000);
            i++;
            if (sub.sql.isEmpty()) {
                return new ScopeCondition(); // 一条 grant unbounded → 并集 unbounded (放行)
            }
            if (DENY.equals(sub.sql)) {
                continue; // deny grant 不贡献 OR
            }
            parts.add(sub.sql);
            combined.params.addAll(sub.params);
        }
        if (parts.isEmpty()) {
            combined.sql = DENY; // 全 deny
            return combined;
        }
        combined.sql = parts.size() == 1 ? parts.get(0) : "(" + String.join(" OR ", parts) + ")";
        return combined;
    }

    /** spec 的 grant 列表: relation_grants 非空则用; 否则由轴① bridge 派生单 grant (R3a 过渡)。 */
    private List<RelationGrant> grantsOf(ScopeSpec spec, ResourceScopeMeta meta) {
        if (spec.hasRelationGrants()) {
            return spec.getRelationGrants();
        }
        OrgAnchor anchor = spec.getOrgAnchor() != null ? spec.getOrgAnchor() : OrgAnchor.SELF;
        return RelationGrant.fromM1Axes(anchor, spec.getAnchorParam(), spec.isIncludeSubtree(),
                spec.getCustomOrgIds(), meta.viaMembership());
    }

    /**
     * 一条 grant → 子条件 (R4): grant.relation 的 storage_kind=RECORD_RELATION → record_relations
     * 子查询; 否则走 org-anchor 单锚点 compose ({@link #composeAnchorCondition})。
     */
    private ScopeCondition composeGrant(RelationGrant g, ScopeSpec spec, ResourceScopeMeta meta,
            UserContext ctx, Long effectiveOrgId, String effectiveOrgPath, Long tenantId, int paramOffset) {
        // P1: 有中间跳 → 多级关系链 (我 →[hops]→ 组织集 S, relation 作终端 over S)。
        // 空跳 (既有全部配置) 走下方旧路径, 字节不变 → 金标准安全。
        //
        // [完成项5 — 双路径有意共存, 非"未统一"债务] 链是规范模型; 1 跳 grant 是它的"退化优化路径":
        // 退化链 (hops 空) 的等价形态 = {hops:[{subject 关系→org}], terminal:relation}, 已 2b live shadow
        // 实证两路径同行集 ({owner_org,RELATION,X} ≡ 链 {[X]→org, owner_org})。下方走 composeAnchorCondition
        // 复用历经金标准锤炼的 resolveOrgSet (ALL/CUSTOM/PLUGIN_DIM/MY_ORG/SELF 等非 access_relations 解析),
        // 故意保留为快路径 —— "退役 composeGrant 改由链编译器重实现这些解析" = 赌金标准换零功能收益, 不做。
        if (g.hasHops()) {
            return composeHopChain(g, spec, meta, ctx, tenantId, paramOffset);
        }
        if (isRecordRelation(meta, g)) {
            return buildRecordRelationCondition(g, spec, meta, ctx, paramOffset);
        }
        StorageKind providerKind = providerStorage(meta, g);
        if (providerKind == StorageKind.PROVIDER) {
            return buildProviderCondition(g, spec, meta, ctx, tenantId, paramOffset);
        }
        // A1 per-relation COLUMN: 非 owner_org/creator 的 COLUMN 锚点 (如 inspection_submission.inspected→target_id,
        // inspection_task.reviewer→reviewer_id) 挂在该关系注册的列上, 而非默认 owner_org/creator 列。临时换列后复用单锚点 compose。
        ResourceScopeMeta effMeta = perRelationColumnMeta(meta, g, providerKind);
        return composeAnchorCondition(toSubSpec(g, spec), effMeta, ctx,
                effectiveOrgId, effectiveOrgPath, tenantId, paramOffset);
    }

    /**
     * A1: grant.relation 是非 owner_org/creator 的 COLUMN 锚点 → 返回把 org/creator 列都换成该关系注册列的 meta
     * 副本(让 SELF→{@code col=me}、org-set→{@code col IN(S)}); 否则原样返回。
     */
    private ResourceScopeMeta perRelationColumnMeta(ResourceScopeMeta meta, RelationGrant g, StorageKind providerKind) {
        if (providerKind != StorageKind.COLUMN
                || ResourceRelationRegistry.OWNER_ORG.equals(g.relation())
                || ResourceRelationRegistry.CREATOR.equals(g.relation())) {
            return meta;
        }
        String col = resourceRelationRegistry.relationOf(meta.resourceCode(), g.relation())
                .map(ResourceRelationRegistry.AnchorRow::columnName).orElse(null);
        return (col == null || col.isBlank()) ? meta : meta.withColumn(col);
    }

    /**
     * P1 多级关系链 grant → 子条件: 中间跳 ({@link ChainCompiler} via {@link ChainHopResolver}) 求可达组织集 S,
     * grant.relation 作终端 over S (COLUMN: col IN(S) / SUBJECT_GRAPH: 成员 of S)。命名参数内联为位置 ?,
     * 再叠加轴③ typeFilter (复用既有, 与旧路径一致)。
     */
    private ScopeCondition composeHopChain(RelationGrant g, ScopeSpec spec, ResourceScopeMeta meta,
                                           UserContext ctx, Long tenantId, int paramOffset) {
        // 单终端链: hops + relation 作唯一终端锚点 (多终端 AND 是后续细化)
        // [完成项2] 链 grant 复用 subjectParam 携带终端成员关系 (属于/负责; 空→member); subject 已归一 SELF。
        com.school.management.domain.access.model.chain.Chain chain =
                new com.school.management.domain.access.model.chain.Chain(
                        g.hops(),
                        new com.school.management.domain.access.model.chain.Terminal(
                                java.util.List.of(g.relation()),
                                com.school.management.domain.access.model.chain.Combine.OR,
                                g.subjectParam()),
                        java.util.List.of());
        com.school.management.infrastructure.extension.SqlFragment frag =
                chainCompiler.compileChain(chain, meta, ctx.getUserId(), tenantId == null ? 1L : tenantId);
        ScopeCondition cond = new ScopeCondition();
        String sql = frag.sql();
        if (sql == null || sql.isBlank() || "1=0".equals(sql.replace(" ", ""))) {
            cond.sql = DENY; // 链 deny → fail-closed
            return cond;
        }
        cond.sql = inlineNamedParams(sql, frag.params(), cond, paramOffset);
        return typeFilter(cond, spec, meta, paramOffset);
    }

    /** grant.relation 在该资源是否 RECORD_RELATION 存储 (查 registry per-relation)。resourceCode 空→false。 */
    private boolean isRecordRelation(ResourceScopeMeta meta, RelationGrant g) {
        if (meta.resourceCode() == null || meta.resourceCode().isEmpty()) {
            return false;
        }
        return resourceRelationRegistry.relationOf(meta.resourceCode(), g.relation())
                .map(r -> r.storageKind() == StorageKind.RECORD_RELATION)
                .orElse(false);
    }

    /** grant.relation 的存储种类 (供 PROVIDER 分流)；resourceCode 空 / 未注册 → null。 */
    private StorageKind providerStorage(ResourceScopeMeta meta, RelationGrant g) {
        if (meta.resourceCode() == null || meta.resourceCode().isEmpty()) {
            return null;
        }
        return resourceRelationRegistry.relationOf(meta.resourceCode(), g.relation())
                .map(ResourceRelationRegistry.AnchorRow::storageKind)
                .orElse(null);
    }

    /**
     * PROVIDER grant → 插件 resolver 算可见记录集 (R3c)。
     *
     * <p>取 {@code resolver_bean} 指向的 {@link com.school.management.infrastructure.extension.RecordRelationResolver},
     * 优先用 {@code subquery()} 包成 {@code {alias}id IN (<子查询>)} (命名参数 :name 按序改写为位置 ?);
     * 否则用 {@code recordIds()} 包成 {@code {alias}id IN (...)}。<b>fail-closed</b>:bean 不可用 / 两者皆空 /
     * id 集为空 → emit {@code 1=0}。仍叠加轴③ typeFilter。
     */
    private ScopeCondition buildProviderCondition(RelationGrant g, ScopeSpec spec,
            ResourceScopeMeta meta, UserContext ctx, Long tenantId, int paramOffset) {
        ScopeCondition cond = new ScopeCondition();
        String alias = meta.aliasPrefix();
        String beanName = resourceRelationRegistry.relationOf(meta.resourceCode(), g.relation())
                .map(ResourceRelationRegistry.AnchorRow::resolverBean).orElse(null);

        var resolverOpt = recordRelationResolverRouter.resolve(beanName);
        if (resolverOpt.isEmpty()) {
            cond.sql = DENY;   // fail-closed: resolver 不可用
            return cond;
        }
        var resolver = resolverOpt.get();
        var sctx = new com.school.management.infrastructure.extension.ScopeContext(
                ctx.getUserId(), "USER", meta.resourceCode(), tenantId);

        var frag = resolver.subquery(sctx);
        if (frag != null && frag.sql() != null && !frag.sql().isBlank()) {
            String inlined = inlineNamedParams(frag.sql(), frag.params(), cond, paramOffset);
            cond.sql = alias + "id IN (" + inlined + ")";
            return typeFilter(cond, spec, meta, paramOffset);
        }

        List<Long> ids = resolver.recordIds(sctx);
        if (ids == null || ids.isEmpty()) {
            cond.sql = DENY;   // fail-closed: 无子查询且无 id 集
            return cond;
        }
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) in.append(", ");
            in.append("?");
            cond.addParam("_dp_pid_" + paramOffset + "_" + i, ids.get(i), Long.class, JdbcType.BIGINT);
        }
        cond.sql = alias + "id IN (" + in + ")";
        return typeFilter(cond, spec, meta, paramOffset);
    }

    /**
     * 把命名参数子查询 ({@code :name}) 按出现顺序改写为位置 {@code ?},并依序把值加入 cond.params
     * (property 唯一名 {@code _dp_pv_<offset>_<i>})。命名参数可重复出现 → 每次出现各加一个 ?。
     */
    private String inlineNamedParams(String sql, java.util.Map<String, Object> params,
                                     ScopeCondition cond, int paramOffset) {
        java.util.regex.Matcher m = NAMED_PARAM.matcher(sql);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (m.find()) {
            Object val = params == null ? null : params.get(m.group(1));
            cond.addParam("_dp_pv_" + paramOffset + "_" + (i++), val,
                    val == null ? Object.class : val.getClass(), jdbcTypeOf(val));
            m.appendReplacement(sb, "?");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static final java.util.regex.Pattern NAMED_PARAM =
            java.util.regex.Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");

    private static JdbcType jdbcTypeOf(Object v) {
        if (v instanceof Long || v instanceof Integer || v instanceof Short) return JdbcType.BIGINT;
        if (v instanceof Boolean) return JdbcType.BOOLEAN;
        return JdbcType.VARCHAR;
    }

    /**
     * RECORD_RELATION grant → {@code record_relations} 子查询 (R4)。
     *
     * <p>语义 subject=SELF: "我经该关系 (reviewer/inspected/...) 可见的记录"。emit:
     * <pre>{alias}id IN (SELECT record_id FROM record_relations WHERE resource_code=? AND relation_code=?
     *   AND subject_type='USER' AND subject_id=? AND deleted=0 AND (valid_to IS NULL OR valid_to>NOW()))</pre>
     * 仍叠加轴③ typeFilter (与其它 grant 一致)。⚠ 仅处理 subject=SELF (用户即关系主体); 非 SELF 的
     * RECORD_RELATION (如 org 作 reviewer) 罕见, 留后续。
     */
    private ScopeCondition buildRecordRelationCondition(RelationGrant g, ScopeSpec spec,
            ResourceScopeMeta meta, UserContext ctx, int paramOffset) {
        ScopeCondition cond = new ScopeCondition();
        String alias = meta.aliasPrefix();
        cond.sql = alias + "id IN (SELECT record_id FROM record_relations WHERE resource_code = ? "
                + "AND relation_code = ? AND subject_type = 'USER' AND subject_id = ? AND deleted = 0 "
                + "AND (valid_to IS NULL OR valid_to > NOW()))";
        cond.addParam("_dp_rrRes_" + paramOffset, meta.resourceCode(), String.class, JdbcType.VARCHAR);
        cond.addParam("_dp_rrRel_" + paramOffset, g.relation(), String.class, JdbcType.VARCHAR);
        cond.addParam("_dp_rrSubj_" + paramOffset, ctx.getUserId(), Long.class, JdbcType.BIGINT);
        return typeFilter(cond, spec, meta, paramOffset);
    }

    /** 一条 grant → 等价单锚点 sub-spec (轴① from grant; 轴②③ 从父 spec 透传)。 */
    private ScopeSpec toSubSpec(RelationGrant g, ScopeSpec parent) {
        return ScopeSpec.builder()
                .applyTo(parent.getApplyTo())
                .orgAnchor(anchorFromSubject(g.subject()))
                .anchorParam(g.subjectParam())
                .includeSubtree(g.subtree())
                .customOrgIds(g.orgIds())
                .subjectRelInclude(parent.getSubjectRelInclude())
                .subjectRelExclude(parent.getSubjectRelExclude())
                .typeFilter(parent.getTypeFilter())
                .build();
    }

    /** SubjectScope → 等价 OrgAnchor (供复用单锚点 compose)。 */
    private OrgAnchor anchorFromSubject(SubjectScope s) {
        return switch (s) {
            case SELF -> OrgAnchor.SELF;
            case MY_ORG -> OrgAnchor.PRIMARY_ORG;
            case RELATION -> OrgAnchor.RELATION;
            case CUSTOM -> OrgAnchor.CUSTOM_ORG;
            case PLUGIN_DIM -> OrgAnchor.PLUGIN_DIM;
            case ALL -> OrgAnchor.ALL;
        };
    }

    /**
     * compose 一个单锚点 spec → 一个 ScopeCondition。
     *
     * <p><b>per-role effective org (T7 关键)</b>: 锚点 org 由调用方<b>显式</b>传入
     * ({@code effectiveOrgId} / {@code effectiveOrgPath}), 而非从 {@code ctx} 直接读取。
     * 这保留 interceptor 旧 {@code buildScopedCondition} 的语义: ORG_UNIT scope-type 的角色用
     * 该角色的 scope org 作锚点, 其它 (ALL scope-type) 用用户主组织。PRIMARY_ORG 解析与
     * access_relation 的 org-subject-OR / orgField-direct-OR 一律消费这两个参数。
     * SELF / creator / RELATION / plugin-dim self 仍用 {@code ctx.getUserId()} (与角色 org 无关)。
     *
     * @param spec            三轴数据范围规格
     * @param meta            资源侧元数据 (别名/org 字段/creator/resourceType/viaMembership/...)
     * @param ctx             当前用户上下文 (仅用于 userId 与 plugin-dim resolve)
     * @param effectiveOrgId  该角色的有效锚点 org id (ORG_UNIT scope → 角色 scope id; 否则 ctx 主组织)
     * @param effectiveOrgPath该角色的有效锚点 org path (同上)
     * @param tenantId        租户
     * @param paramOffset     参数名唯一化偏移 (多角色时每角色递增)
     */
    private ScopeCondition composeAnchorCondition(ScopeSpec spec, ResourceScopeMeta meta,
                                         UserContext ctx, Long effectiveOrgId, String effectiveOrgPath,
                                         Long tenantId, int paramOffset) {
        // 轴① 解析 org id 集合 (单一真相)
        OrgSet orgSet = resolveOrgSet(spec, meta, ctx, effectiveOrgId, effectiveOrgPath, tenantId, paramOffset);

        // PLUGIN_DIM 的 deny / inline-id / self-degrade 是终态, 直接由 resolveOrgSet 产出完整 cond
        if (orgSet.terminalCond != null) {
            // 终态仍要叠加类型过滤 (插件维度路径也受闸2/2b约束 — 与 interceptor 一致)
            return typeFilter(orgSet.terminalCond, spec, meta, paramOffset);
        }

        // 轴① 消费: 按资源路径生成 subjectSelect 谓词
        ScopeCondition cond = subjectSelect(orgSet, spec, meta, ctx, effectiveOrgId, effectiveOrgPath, tenantId, paramOffset);

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
                                 UserContext ctx, Long effectiveOrgId, String effectiveOrgPath,
                                 Long tenantId, int paramOffset) {
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
                // per-role effective org (T7): 不再读 ctx 主组织, 用调用方传入的有效锚点。
                Long orgId = effectiveOrgId;
                String orgPath = effectiveOrgPath;
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
        // R4: plugin-dim resolve 的"资源"用 resourceCode (=moduleCode, 恒有); 不再靠 interceptor 把
        // moduleCode 注入 resourceType (那会污染同 spec 的非 PLUGIN_DIM grant → 误走 accessRelationSelect)。
        String resourceCode = meta.resourceCode();
        ScopeCondition cond = new ScopeCondition();

        List<Long> ids = pluginDataScopeRouter.resolve(dimCode, ctx.getUserId(), resourceCode);

        if (ids == null) {
            log.warn("[DataPermission] plugin dim '{}' unavailable, degrading to SELF", dimCode);
            String selfField;
            if (meta.viaMembership()) {
                // 注意: 此分支缺省为 "user_id" (端口自 interceptor 既有口径), 与 membershipSelect /
                // subjectRelFilter 的 "id" 缺省不同 —— 有意保留差异, 不收敛到 membershipSubjectColumnOrDefault()。
                selfField = meta.membershipSubjectColumn() == null || meta.membershipSubjectColumn().isEmpty()
                        ? "user_id" : meta.membershipSubjectColumn();
            } else {
                // [B-2 镜像] 无 created_by 列的资源 (系统计算表如 org_unit_scores/student_grade/school_class)
                // 降级 SELF 无从锚定 → DENY (空集, 安全), 而非拼出 "t.created_by = ?" 在无该列的表上崩 SQL 1054。
                // 与 orgFieldSelect 的 SELF 分支对称; creatorFieldOrDefault() 的 "created_by" 兜底只对有该列的资源安全。
                String cf = meta.creatorField();
                if (cf == null || cf.isBlank()) {
                    cond.sql = DENY;
                    return cond;
                }
                selfField = cf;
            }
            cond.sql = alias + selfField + " = ?";
            cond.addParam("_dp_pluginSelf_" + paramOffset, ctx.getUserId(), Long.class, JdbcType.BIGINT);
            return cond;
        }
        if (ids.isEmpty()) {
            cond.sql = DENY;
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
                                         UserContext ctx, Long effectiveOrgId, String effectiveOrgPath,
                                         Long tenantId, int paramOffset) {
        if (orgSet.unbounded) {
            return new ScopeCondition(); // ALL → 空 (无过滤)
        }

        if (meta.viaMembership()) {
            return membershipSelect(orgSet, spec, meta, ctx, tenantId, paramOffset);
        }
        // R4 后清理: 旧 accessRelationSelect (resourceType 非空走 access_relations 子查询) 已删 —— resourceType
        // 全无来源 (无注解设, data_resources.access_resource_type 全 NULL; 经核实) → 该分支 production 不可达,
        // 是 registry 化 (viaMembership) 之前的 legacy 路径。effectiveOrgId/Path 此后仅 resolveOrgSet 用。
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
            // [B-2] 无 creator/owner-user 列的资源 (系统计算表如 org_unit_scores/student_grade/school_class):
            // "仅本人/我创建的" 无从锚定 → DENY (空集, 安全), 而非拼出 "t.null = ?" 崩 SQL (修无 created_by 表
            // SELF 既有 1054/500 类)。有 creator 列的资源 (绝大多数) creatorField 非空, 不受影响。
            if (creatorField == null || creatorField.isBlank()) {
                cond.sql = DENY;
                return cond;
            }
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
    private ScopeCondition membershipSelect(OrgSet orgSet, ScopeSpec spec, ResourceScopeMeta meta,
                                            UserContext ctx, Long tenantId, int paramOffset) {
        String alias = meta.aliasPrefix();
        String subjectCol = meta.membershipSubjectColumnOrDefault();
        ScopeCondition cond = new ScopeCondition();

        if (orgSet.self) {
            cond.sql = alias + subjectCol + " = ?";
            cond.addParam("_dp_self_" + paramOffset, ctx.getUserId(), Long.class, JdbcType.BIGINT);
            return cond;
        }
        if (orgSet.predicate == null) {
            // 无可锚定 org → deny all (避免泄露全表, 与 interceptor membership else 分支一致)
            cond.sql = DENY;
            return cond;
        }

        // ar.tenant_id 必须排在 org 谓词参数之前 (位置绑定) —— interceptor 通过把 tenant param
        // 插到 list 头部实现。这里我们顺序天然正确: 先 add ar.tenant_id, 再 emit org predicate。
        cond.addParam("_dp_memArTenant_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);

        // CUSTOM_ORG 在 membership 路径必须<b>无条件子树展开</b> (端口自 buildMembershipCondition CUSTOM)。
        // 成员挂在叶子组织 (学生是其 CLASS 的 member, 非祖先 GRADE), 授某祖先 org 必须触达其后代叶子
        // 的成员, 故忽略 includeSubtree 一律展开。org-field 路径仍 honor includeSubtree (不变)。
        String orgPredicate;
        if (spec.getOrgAnchor() == OrgAnchor.CUSTOM_ORG) {
            String csv = spec.getCustomOrgIds().stream().map(String::valueOf).collect(Collectors.joining(","));
            orgPredicate = "ar.resource_id IN ("
                    + "SELECT o.id FROM org_units o "
                    + "JOIN org_units g ON g.id IN (" + csv + ") "
                    + "WHERE o.tenant_id = ? AND o.deleted = 0 "
                    + "AND o.tree_path LIKE CONCAT(g.tree_path, '%'))";
            cond.addParam("_dp_memCustomTenant_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);
        } else {
            orgPredicate = orgSet.predicate.emit("ar.resource_id", cond, tenantId, paramOffset);
        }

        cond.sql = alias + subjectCol + " IN ("
                + "SELECT ar.subject_id FROM access_relations ar "
                + "WHERE ar.relation = 'member' AND ar.resource_type = 'org_unit' "
                + "AND ar.subject_type = 'user' AND ar.deleted = 0 AND ar.tenant_id = ? "
                + "AND " + orgPredicate + ")";
        return cond;
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
        if (cond.sql.isEmpty() || DENY.equals(cond.sql)) {
            return cond;
        }

        String alias = meta.aliasPrefix();
        String subjectCol = meta.membershipSubjectColumnOrDefault();

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

        // org 锚定: 与 subjectSelect 同集 (re-derive)。unbounded → 无 org 约束; self → 同样不追加 org 约束
        // (membership SELF + relInclude 会走到此分支, 此时仅按关系码过滤, 不再叠加 org 锚定)。
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
        if (DENY.equals(cond.sql)) {
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
        c.sql = DENY;
        return c;
    }
}
