package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.StorageKind;
import com.school.management.domain.access.model.chain.Chain;
import com.school.management.domain.access.model.chain.Hop;
import com.school.management.domain.access.model.chain.ScopeChainSpec;
import com.school.management.domain.access.model.chain.Terminal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 多级关系链校验器 (统一锚定 P0) —— 保存链配置前的合法性闸。
 *
 * <p>三条硬规则 (设计 §3/§8 P0):
 * <ol>
 *   <li><b>终端∈注册表</b>: 每个 {@link Terminal#anchorRelations} 必须 ∈ 该资源在 {@code resource_relations}
 *       注册的关系 —— 用户要求"链接最后只能是模块数据支持的锚点", 也是可强制执行 SQL 的边界。</li>
 *   <li><b>限深 ≤ {@link #MAX_DEPTH}</b>: 中间跳数上界 → 行级拦截器 worst case 有界 (性能/防爆)。</li>
 *   <li><b>结构合法</b>: 跳关系非空、toType ∈ 三大主体、终端非空。</li>
 * </ol>
 *
 * <p>纯校验, 不碰引擎/拦截器 —— 零金标准回归风险。引擎 (P1) 编译前先过此闸。
 */
@Component
public class ChainValidator {

    /** 中间跳限深 (设计 §4 硬约束); 超过编译期/保存期拒绝, 防递归爆炸。 */
    public static final int MAX_DEPTH = 3;

    /** 三大主体 (relation_types 的 from/to 全集)。 */
    private static final Set<String> ENTITY_TYPES = Set.of("user", "org_unit", "place");

    private final ResourceRelationRegistry registry;

    public ChainValidator(ResourceRelationRegistry registry) {
        this.registry = registry;
    }

    /** 校验结果: valid + 人话错误清单。 */
    public record Result(boolean valid, List<String> errors) {
        public static Result ok() {
            return new Result(true, List.of());
        }
    }

    /**
     * 校验一个资源的链范围配置。
     *
     * @param resourceCode 资源码 (查其注册的终端锚点集)
     * @param spec         链范围 (顶层多链 OR)
     */
    public Result validate(String resourceCode, ScopeChainSpec spec) {
        List<String> errs = new ArrayList<>();
        if (spec == null || spec.chains().isEmpty()) {
            return new Result(false, List.of("数据范围不能为空 (至少一条链)"));
        }

        // 该资源注册的终端锚点候选 + 存储种类 (来自 resource_relations; 数据驱动, 无硬编码)
        Set<String> registered = new java.util.HashSet<>();
        Map<String, StorageKind> kindByRel = new HashMap<>();
        for (ResourceRelationRegistry.AnchorRow row : registry.relationsOf(resourceCode)) {
            registered.add(row.relationCode());
            kindByRel.put(row.relationCode(), row.storageKind());
        }

        List<Chain> chains = spec.chains();
        for (int ci = 0; ci < chains.size(); ci++) {
            Chain c = chains.get(ci);
            String p = "链[" + ci + "]";

            // ① 终端∈注册表
            Terminal t = c.terminal();
            if (t == null || t.anchorRelations().isEmpty()) {
                errs.add(p + " 缺终端锚点 (数据怎么挂到链末实体)");
            } else {
                for (String a : t.anchorRelations()) {
                    if (!registered.contains(a)) {
                        errs.add(p + " 终端锚点 '" + a + "' 未在资源 '" + resourceCode
                                + "' 的 resource_relations 注册 — 终端只能是模块数据支持的锚点"
                                + " (可选: " + registered + ")");
                    } else {
                        // PROVIDER/RECORD 终端自带记录解析, 不消费中间跳组织集 → 须 hops 空
                        StorageKind k = kindByRel.get(a);
                        if ((k == StorageKind.PROVIDER || k == StorageKind.RECORD_RELATION)
                                && !c.hops().isEmpty()) {
                            errs.add(p + " 终端 '" + a + "' (" + k + ") 不支持中间跳 — 此类终端自带记录解析,"
                                    + " 须无中间跳 (hops 空)");
                        }
                    }
                }
            }

            // ② 限深
            List<Hop> hops = c.hops();
            if (hops.size() > MAX_DEPTH) {
                errs.add(p + " 中间跳 " + hops.size() + " 超过限深 " + MAX_DEPTH + " (防递归爆炸/性能)");
            }

            // ③ 结构: 每跳关系非空 + toType 合法
            String prevType = "user"; // 起点恒为用户
            for (int hi = 0; hi < hops.size(); hi++) {
                Hop h = hops.get(hi);
                boolean validType = h.toType() != null && ENTITY_TYPES.contains(h.toType());
                if (!validType) {
                    errs.add(p + " 跳[" + hi + "] 到达类型非法: '" + h.toType() + "' (须 ∈ " + ENTITY_TYPES + ")");
                }
                // [审计#3] place→org 跳走 effective_org_unit_id 投影 (引擎忽略关系码) → 不要求关系非空;
                // 其余跳走 access_relations, 必须有关系。
                boolean placeToOrg = "place".equals(prevType) && "org_unit".equals(h.toType());
                if (!placeToOrg && h.relations().isEmpty()) {
                    errs.add(p + " 跳[" + hi + "] 关系为空");
                }
                if (validType) {
                    prevType = h.toType();
                }
            }
        }
        return new Result(errs.isEmpty(), errs);
    }
}
