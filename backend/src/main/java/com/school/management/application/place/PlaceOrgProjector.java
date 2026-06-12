package com.school.management.application.place;

import com.school.management.application.access.AccessRelationService.RelationAssignedEvent;
import com.school.management.application.access.AccessRelationService.RelationRevokedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 场所有效组织投影器 — {@code places.effective_org_unit_id} 投影列的<b>唯一属主</b>.
 *
 * <p>归属真相在 access_relations 的 {@code belongs_to | place | org_unit} 覆盖点关系;
 * 本列是查询/数据权限快路径的物化投影 (同 {@code org_units.tree_path} 模式),
 * 业务代码<b>禁止直写</b> (PO 层 {@code FieldStrategy.NEVER} 兜底).
 *
 * <p>触发: ① belongs_to 关系 grant/revoke — 同步 {@link EventListener} 同事务重算
 * (派生数据需与变更原子一致, 不适用跨 service 边界的 AFTER_COMMIT 约定);
 * ② 场所移动父节点 / 新建场所 — 调用方显式调 {@link #recomputeSubtree}.
 *
 * <p>重算语义: 子树根的基准 = 父场所当前 effective (子树外, 列值即真),
 * 树内每节点 effective = COALESCE(自身覆盖点, 父 effective)。覆盖点读关系时
 * <b>忽略 valid_to</b> — belongs_to 不支持时效 (投影器无法感知过期, 字典层已声明禁用)。
 * 只写 places 不发关系事件, 无环。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceOrgProjector {

    private final JdbcTemplate jdbcTemplate;
    private final PlaceOrgResolver placeOrgResolver;

    @EventListener
    public void onRelationAssigned(RelationAssignedEvent e) {
        if (isPlaceBelonging(e.relation(), e.subjectType())) {
            recomputeSubtree(e.subjectId());
        }
    }

    @EventListener
    public void onRelationRevoked(RelationRevokedEvent e) {
        if (isPlaceBelonging(e.relation(), e.subjectType())) {
            recomputeSubtree(e.subjectId());
        }
    }

    private boolean isPlaceBelonging(String relation, String subjectType) {
        return "belongs_to".equals(relation) && "place".equals(subjectType);
    }

    /**
     * 重算以 placeId 为根的子树投影. 场所不存在/已删则 no-op.
     * 一条 path LIKE 查询拉子树 + 一条 IN 查询拉覆盖点 + 批量 UPDATE 差异行.
     */
    @Transactional
    public void recomputeSubtree(Long placeId) {
        if (placeId == null) return;

        Map<String, Object> root;
        try {
            root = jdbcTemplate.queryForMap(
                "SELECT id, parent_id, path, effective_org_unit_id FROM places WHERE id = ? AND deleted = 0",
                placeId);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return;
        }

        // 1. 基准: 父场所当前 effective (父在子树外, 其列值即当前真值); 根场所基准为 NULL
        Long parentId = toLong(root.get("parent_id"));
        Long base = null;
        if (parentId != null) {
            base = jdbcTemplate.query(
                "SELECT effective_org_unit_id FROM places WHERE id = ? AND deleted = 0",
                rs -> rs.next() ? toLong(rs.getObject(1)) : null,
                parentId);
        }

        // 2. 拉子树 (path 前缀含自身), 按 level/path 排序保证父先于子
        String rootPath = (String) root.get("path");
        List<Map<String, Object>> rows = (rootPath == null)
            ? List.of(root)
            : jdbcTemplate.queryForList(
                "SELECT id, parent_id, path, effective_org_unit_id FROM places " +
                "WHERE path LIKE CONCAT(?, '%') AND deleted = 0 ORDER BY path",
                rootPath);

        // 3. 覆盖点 (一条 IN 查询)
        List<Long> ids = rows.stream().map(r -> toLong(r.get("id"))).toList();
        Map<Long, Long> overrides = placeOrgResolver.overridesFor(ids);

        // 4. 内存解析: effective = COALESCE(覆盖点, 父 effective); 父不在子树内 → 用基准
        Map<Long, Long> resolved = new LinkedHashMap<>();
        Map<Long, Long> current = new HashMap<>();
        List<Object[]> diffs = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long id = toLong(row.get("id"));
            Long pid = toLong(row.get("parent_id"));
            Long parentEffective = resolved.containsKey(pid) ? resolved.get(pid) : base;
            Long effective = overrides.containsKey(id) ? overrides.get(id) : parentEffective;
            resolved.put(id, effective);
            current.put(id, toLong(row.get("effective_org_unit_id")));
            if (!Objects.equals(effective, current.get(id))) {
                diffs.add(new Object[]{effective, id});
            }
        }

        // 5. 批量写差异行 (投影器是本列唯一写入方)
        if (!diffs.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "UPDATE places SET effective_org_unit_id = ? WHERE id = ?", diffs);
            log.info("[PlaceOrgProjector] recomputeSubtree root={} 共 {} 节点, 更新 {} 行",
                placeId, rows.size(), diffs.size());
        }
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }
}
