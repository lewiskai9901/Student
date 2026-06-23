package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.StorageKind;
import com.school.management.domain.access.model.chain.Chain;
import com.school.management.domain.access.model.chain.Combine;
import com.school.management.infrastructure.extension.SqlFragment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关系链编译器 (统一锚定 P1 Step2a) —— 一条 {@link Chain} → 资源表上的 SQL 谓词。
 *
 * <p>管线: 中间跳 ({@link ChainHopResolver}) → 可达末实体集 S 子查询; 终端按 {@code storage_kind}
 * 把数据挂到 S 上 (COLUMN: {@code data.col IN (S)}; SUBJECT_GRAPH: 成员图 {@code data.id IN (member of S)})。
 * 多终端锚点按 {@link com.school.management.domain.access.model.chain.Terminal#combine} AND/OR 叠加。
 *
 * <p><b>本步范围 (2a)</b>: COLUMN + SUBJECT_GRAPH 终端 + 多跳 + AND/OR; 加性, 不接 live (拦截器仍走旧 grant
 * 路径, 金标准不动)。PROVIDER/RECORD 终端 + grant→chain 转换 + 接 live + shadow 等价比对留 2b/2c。
 *
 * <p>SUBJECT_GRAPH 终端的成员关系暂固定 {@code member} (与旧引擎 ScopeEvaluator:595 一致); 可选 membership
 * 关系 (属于/负责) 留后续。
 */
@Component
public class ChainCompiler {

    static final String DENY = "1=0";

    private final ChainHopResolver hopResolver;
    private final ResourceRelationRegistry registry;

    public ChainCompiler(ChainHopResolver hopResolver, ResourceRelationRegistry registry) {
        this.hopResolver = hopResolver;
        this.registry = registry;
    }

    /**
     * 编译一条链 → 资源谓词。
     *
     * <p><b>终端列取自 {@code meta}</b> (非注册表 columnName): owner_org→{@link ResourceScopeMeta#orgUnitField()}
     * / creator→{@link ResourceScopeMeta#creatorField()} / SUBJECT_GRAPH→{@code membershipSubjectColumnOrDefault()}。
     * 这样 READ/UPDATE/DELETE 用真资源列 (org_unit_id), 而 INSERT 授权探针传入"合成 org 元"(orgUnitField=id,
     * 探 org_units) 时自然产 {@code id IN (S)} —— 同一编译器两用。registry 仅供判 storage_kind。
     *
     * @param chain   链
     * @param meta    资源元 (别名/org列/creator列/成员主体列/资源码); INSERT 探针传合成 org 元
     * @param userId  当前用户 (链起点)
     * @param tenantId 租户 (成员图子查询 ar.tenant_id 过滤)
     * @return SQL 谓词 + 命名参数; {@code 1=0} = deny
     */
    public SqlFragment compileChain(Chain chain, ResourceScopeMeta meta, long userId, long tenantId) {
        Map<String, Object> params = new LinkedHashMap<>();

        // 中间跳 → 可达末实体集 S 子查询; hops 空 → S=null (终端直接绑用户)
        String sSubquery = null;
        if (!chain.hops().isEmpty()) {
            SqlFragment hop = hopResolver.resolve(chain.hops(), userId);
            sSubquery = hop.sql();
            params.putAll(hop.params());
        }

        // 终端: 每个锚点一条谓词; SUBJECT_GRAPH 用链声明的成员关系 (属于/负责, 默认 member)
        String membershipRel = chain.terminal().membershipRelationOrDefault();
        // [完成项4] 成员图终端的实体类型 = 链末跳到达类型 (org_unit 默认 / place → 场所占用); S 即该类型实体集
        String lastHopType = chain.hops().isEmpty() ? "org_unit"
                : chain.hops().get(chain.hops().size() - 1).toType();
        List<String> preds = new ArrayList<>();
        int ai = 0;
        for (String anchor : chain.terminal().anchorRelations()) {
            String pred = terminalPredicate(anchor, meta, membershipRel, lastHopType, sSubquery, userId, tenantId, params, ai++);
            if (pred != null) {
                preds.add(pred);
            }
        }
        if (preds.isEmpty()) {
            return SqlFragment.of(DENY, params);
        }
        String joiner = chain.terminal().combine() == Combine.AND ? " AND " : " OR ";
        String sql = preds.size() == 1 ? preds.get(0) : "(" + String.join(joiner, preds) + ")";
        return SqlFragment.of(sql, params);
    }

    /** 三大主体白名单 (成员图 resource_type 内联前校验, 防注入)。 */
    private static final java.util.Set<String> ENTITY_TYPES = java.util.Set.of("user", "org_unit", "place");

    /** 一个终端锚点 → 谓词 (按 storage_kind)。null = 跳过 (不应发生; 未注册→DENY)。 */
    private String terminalPredicate(String anchor, ResourceScopeMeta meta, String membershipRel,
                                     String entityType, String sSubquery, long userId, long tenantId,
                                     Map<String, Object> params, int ai) {
        String resourceCode = meta.resourceCode();
        String alias = meta.aliasPrefix();
        var rowOpt = registry.relationOf(resourceCode, anchor);
        if (rowOpt.isEmpty()) {
            return DENY; // 终端未注册 (ChainValidator 本应拦下) → fail-closed
        }
        StorageKind kind = rowOpt.get().storageKind();
        boolean isCreator = ResourceRelationRegistry.CREATOR.equals(anchor);
        boolean isOwnerOrg = ResourceRelationRegistry.OWNER_ORG.equals(anchor);
        switch (kind) {
            case COLUMN: {
                // 列取自 meta (owner_org→orgUnitField / creator→creatorField), 使 INSERT 合成 org 元 (=id) 自然两用;
                // 其它列锚回退注册表列名。
                String col = isOwnerOrg ? meta.orgUnitField()
                        : isCreator ? meta.creatorField()
                        : rowOpt.get().columnName();
                // creator (USER 锚点) = 我创建的 → 绑用户, 与中间跳无关 (created_by = me)
                if (isCreator) {
                    String p = "ccMe" + ai;
                    params.put(p, userId);
                    return alias + col + " = :" + p;
                }
                // owner_org 类 (ORG 锚点) 需中间跳产组织集 S; 无 S → deny (不可"组织=我")
                if (sSubquery == null || col == null || col.isBlank()) {
                    return DENY;
                }
                return alias + col + " IN (" + sSubquery + ")";
            }
            case SUBJECT_GRAPH: {
                // 成员主体列 (如 user_student.user_id) —— 旧引擎 membershipSelect 同口径; shadow 实证非 id
                String subjectCol = meta.membershipSubjectColumnOrDefault();
                if (sSubquery == null) {
                    // hops 空 → 成员自身 (data 即我, subjectCol = me)
                    String p = "ccMe" + ai;
                    params.put(p, userId);
                    return alias + subjectCol + " = :" + p;
                }
                // [完成项4] 成员图实体类型 = 链末跳类型 (org_unit / place 场所占用); 白名单校验后内联
                String resType = ENTITY_TYPES.contains(entityType) ? entityType : "org_unit";
                // 数据(成员主体) ∈ S 中各组织的 [成员关系] ([完成项2/3] 默认 member; 可配单个(属于/负责) 或
                // 逗号多个=AND 交集(属于且负责, 同一组织同时具备); + tenant 过滤)
                String tp = "ccTenant" + ai;
                params.put(tp, tenantId);
                String[] memRels = membershipRel.split(",");
                String relClause;
                String groupBy = "";
                if (memRels.length == 1) {
                    String rp = "ccmRel" + ai;
                    params.put(rp, memRels[0].trim());
                    relClause = "ccm" + ai + ".relation = :" + rp;
                } else {
                    // AND: 同一 (subject, org) 同时具备全部成员关系 → 分组计数
                    List<String> ph = new ArrayList<>();
                    for (int ri = 0; ri < memRels.length; ri++) {
                        String rp = "ccmRel" + ai + "_" + ri;
                        params.put(rp, memRels[ri].trim());
                        ph.add(":" + rp);
                    }
                    relClause = "ccm" + ai + ".relation IN (" + String.join(",", ph) + ")";
                    groupBy = " GROUP BY ccm" + ai + ".subject_id, ccm" + ai + ".resource_id"
                            + " HAVING COUNT(DISTINCT ccm" + ai + ".relation) >= " + memRels.length;
                }
                return alias + subjectCol + " IN (SELECT ccm" + ai + ".subject_id FROM access_relations ccm" + ai
                        + " WHERE " + relClause + " AND ccm" + ai + ".resource_type = '" + resType + "'"
                        + " AND ccm" + ai + ".subject_type = 'user' AND ccm" + ai + ".deleted = 0"
                        + " AND ccm" + ai + ".tenant_id = :" + tp
                        + " AND ccm" + ai + ".resource_id IN (" + sSubquery + ")" + groupBy + ")";
            }
            case PROVIDER:
            case RECORD_RELATION:
                // PROVIDER/RECORD 终端按定义自带记录解析 (插件 resolver / record_relations), 不消费中间跳组织集 S
                // → 仅在 hops 空时有意义, 此时走旧 composeGrant 路径 (非本编译器)。链 (hops 非空) + 这类终端
                // 语义无效 → fail-closed DENY (不崩查询); ChainValidator 在保存期拒绝该组合。
                return DENY;
            default:
                return DENY;
        }
    }
}
