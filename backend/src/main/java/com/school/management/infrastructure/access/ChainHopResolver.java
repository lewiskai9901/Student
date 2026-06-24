package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.chain.Combine;
import com.school.management.domain.access.model.chain.Hop;
import com.school.management.infrastructure.extension.SqlFragment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 关系链中间跳解析器 (统一锚定 P1 Step1) —— 把"我 →[关系]→ 中间实体 →…→ 末实体"的中间跳
 * 编译成一段 {@code access_relations} 嵌套子查询, 产出"可达末实体 id 集"的 SELECT。
 *
 * <p>引擎 (P1 Step2) 再把它喂给终端 (data.owner_org IN (&lt;本子查询&gt;) 等)。本类只管中间跳,
 * 是纯 SQL 构造 (不连库不执行), 与终端/拦截器解耦, 便于单测。
 *
 * <p><b>组合语义</b>: 同一跳多关系 — OR = {@code relation IN (...)} (并集); AND =
 * {@code GROUP BY resource_id HAVING COUNT(DISTINCT relation) >= N} (交集, 可移植, 不依赖 INTERSECT)。
 *
 * <p><b>注入安全</b>: 关系码 + userId 走命名参数; 实体类型 (subject_type/resource_type) 取自链模型
 * 且校验 ∈ 三大主体白名单后内联 (非用户数据)。
 *
 * <p>org_unit 跳含下级 (subtree) → tree_path 子树展开 ([#1] 修复, 与旧 RELATION 锚点同口径); 终端不在此处。
 */
@Component
public class ChainHopResolver {

    /** 三大主体白名单 (内联前校验, 防注入)。 */
    private static final Set<String> ENTITY_TYPES = Set.of("user", "org_unit", "place");

    /**
     * 解析中间跳 → 可达末实体 id 子查询。
     *
     * @param hops   中间跳 (非空; 空 = 终端直接绑用户, 由调用方处理)
     * @param userId 当前用户 (链起点)
     * @return 一段 {@code SELECT resource_id …} 子查询 (命名参数), 选出走完所有跳后可达的末实体 id
     */
    public SqlFragment resolve(List<Hop> hops, long userId) {
        if (hops == null || hops.isEmpty()) {
            throw new IllegalArgumentException("hops 为空: 终端直接绑定用户, 无需中间跳解析");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        String prevType = "user";          // 起点恒为用户
        String inner = null;               // 上一级 id 子查询; level0 为 null (= :me)

        for (int k = 0; k < hops.size(); k++) {
            Hop h = hops.get(k);
            if (h.toType() == null || !ENTITY_TYPES.contains(h.toType())) {
                throw new IllegalArgumentException("跳[" + k + "] 到达类型非法: " + h.toType());
            }

            // P2: place→org_unit 跳走 effective_org_unit_id 投影 (A3 place 归属真相的物化列), 而非
            // access_relations belongs_to —— 投影是 place→org 的单一真相入口, 且免一次 access_relations 子查询。
            // [审计#3] 此跳不读关系码 (投影即真相) → 不要求 relations 非空 (放在 relations-empty 校验之前)。
            // (place→org 必非起跳: prevType=place 仅可能来自上一跳, 故 inner 非 null。)
            if ("place".equals(prevType) && "org_unit".equals(h.toType())) {
                String p = "plc" + k;
                inner = "SELECT " + p + ".effective_org_unit_id FROM places " + p
                        + " WHERE " + p + ".id IN (" + inner + ")"
                        + " AND " + p + ".effective_org_unit_id IS NOT NULL AND " + p + ".deleted = 0";
                prevType = "org_unit";
                if (h.subtree()) {
                    inner = subtreeExpand(inner, k); // [#1] 投影出的组织集再含下级
                }
                continue;
            }

            // 非 place→org 跳走 access_relations, 必须有关系码
            if (h.relations().isEmpty()) {
                throw new IllegalArgumentException("跳[" + k + "] 关系为空");
            }

            String a = "ar" + k;
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ").append(a).append(".resource_id FROM access_relations ").append(a)
              .append(" WHERE ").append(a).append(".subject_type = '").append(prevType).append("'");

            // 起点 = :me; 后续级 = 上一级子查询
            if (inner == null) {
                params.put("chmMe", userId);
                sb.append(" AND ").append(a).append(".subject_id = :chmMe");
            } else {
                sb.append(" AND ").append(a).append(".subject_id IN (").append(inner).append(")");
            }

            // 关系 IN (...) — 每个关系码一个命名参数
            List<String> rels = h.relations();
            List<String> ph = new ArrayList<>(rels.size());
            for (int ri = 0; ri < rels.size(); ri++) {
                String p = "chmH" + k + "r" + ri;
                params.put(p, rels.get(ri));
                ph.add(":" + p);
            }
            sb.append(" AND ").append(a).append(".relation IN (").append(String.join(",", ph)).append(")")
              .append(" AND ").append(a).append(".resource_type = '").append(h.toType()).append("'")
              .append(" AND ").append(a).append(".deleted = 0");

            // AND 交集: 该实体须同时具备全部关系 → 分组计数
            if (h.combine() == Combine.AND && rels.size() > 1) {
                sb.append(" GROUP BY ").append(a).append(".resource_id")
                  .append(" HAVING COUNT(DISTINCT ").append(a).append(".relation) >= ").append(rels.size());
            }

            inner = sb.toString();
            prevType = h.toType();

            // [#1] org_unit 跳含下级 → tree_path 子树展开 (我直接关系到的组织 + 其全部后代)。
            // 与 ScopeEvaluator RELATION/CUSTOM 子树分支同款 tree_path LIKE 口径; 旧 1 跳 RELATION 锚点
            // honor subtree, 多级链此前漏读 h.subtree() → 配"含下级"形同未勾, 此处补齐使两路径一致。
            if ("org_unit".equals(prevType) && h.subtree()) {
                inner = subtreeExpand(inner, k);
            }
        }
        return SqlFragment.of(inner, params);
    }

    /**
     * 子树展开: 把"组织 id 子查询"包成"这些组织 + 其全部后代 id"。
     * <pre>SELECT o.id FROM org_units o JOIN org_units anc ON o.tree_path LIKE CONCAT(anc.tree_path,'%')
     *   WHERE anc.id IN (&lt;种子&gt;) AND o.deleted = 0</pre>
     * (与既有 hop 子查询一致, 不加 tenant 过滤 —— 单租户; 别名按跳号 k 唯一化。)
     */
    private String subtreeExpand(String orgIdSubquery, int k) {
        String o = "osub" + k;
        String anc = "oanc" + k;
        return "SELECT " + o + ".id FROM org_units " + o
                + " JOIN org_units " + anc + " ON " + o + ".tree_path LIKE CONCAT(" + anc + ".tree_path, '%')"
                + " WHERE " + anc + ".id IN (" + orgIdSubquery + ") AND " + o + ".deleted = 0";
    }
}
