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
     * @param chain        链
     * @param resourceCode 资源码 (查终端锚点 storage_kind/column)
     * @param alias        资源表别名前缀 (如 "" 或 "t.")
     * @param userId       当前用户 (链起点)
     * @return SQL 谓词 + 命名参数; {@code 1=0} = deny
     */
    public SqlFragment compileChain(Chain chain, String resourceCode, String alias, long userId) {
        Map<String, Object> params = new LinkedHashMap<>();

        // 中间跳 → 可达末实体集 S 子查询; hops 空 → S=null (终端直接绑用户)
        String sSubquery = null;
        if (!chain.hops().isEmpty()) {
            SqlFragment hop = hopResolver.resolve(chain.hops(), userId);
            sSubquery = hop.sql();
            params.putAll(hop.params());
        }

        // 终端: 每个锚点一条谓词
        List<String> preds = new ArrayList<>();
        int ai = 0;
        for (String anchor : chain.terminal().anchorRelations()) {
            String pred = terminalPredicate(anchor, resourceCode, alias, sSubquery, userId, params, ai++);
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

    /** 一个终端锚点 → 谓词 (按 storage_kind)。null = 跳过 (不应发生; 未注册→DENY)。 */
    private String terminalPredicate(String anchor, String resourceCode, String alias,
                                     String sSubquery, long userId, Map<String, Object> params, int ai) {
        var rowOpt = registry.relationOf(resourceCode, anchor);
        if (rowOpt.isEmpty()) {
            return DENY; // 终端未注册 (ChainValidator 本应拦下) → fail-closed
        }
        StorageKind kind = rowOpt.get().storageKind();
        boolean isCreator = ResourceRelationRegistry.CREATOR.equals(anchor);
        switch (kind) {
            case COLUMN: {
                String col = rowOpt.get().columnName();
                // creator (USER 锚点) = 我创建的 → 绑用户, 与中间跳无关 (created_by = me)
                if (isCreator) {
                    String p = "ccMe" + ai;
                    params.put(p, userId);
                    return alias + col + " = :" + p;
                }
                // owner_org 类 (ORG 锚点) 需中间跳产组织集 S; 无 S → deny (不可"组织=我")
                if (sSubquery == null) {
                    return DENY;
                }
                return alias + col + " IN (" + sSubquery + ")";
            }
            case SUBJECT_GRAPH: {
                if (sSubquery == null) {
                    // hops 空 → 成员自身 (data 即我, 如 user/student 自己)
                    String p = "ccMe" + ai;
                    params.put(p, userId);
                    return alias + "id = :" + p;
                }
                // 数据(成员主体) ∈ S 中各组织的 member
                return alias + "id IN (SELECT ccm" + ai + ".subject_id FROM access_relations ccm" + ai
                        + " WHERE ccm" + ai + ".relation = 'member' AND ccm" + ai + ".resource_type = 'org_unit'"
                        + " AND ccm" + ai + ".subject_type = 'user' AND ccm" + ai + ".deleted = 0"
                        + " AND ccm" + ai + ".resource_id IN (" + sSubquery + "))";
            }
            case PROVIDER:
            case RECORD_RELATION:
                // 2b: 复用 ScopeEvaluator.buildProviderCondition / buildRecordRelationCondition
                throw new UnsupportedOperationException(
                        "PROVIDER/RECORD_RELATION 终端编译留 P1 Step2b: " + resourceCode + "/" + anchor);
            default:
                return DENY;
        }
    }
}
