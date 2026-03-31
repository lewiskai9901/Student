package com.school.management.interfaces.rest.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Asset Category REST Controller
 *
 * Uses JdbcTemplate to operate on: asset_category
 */
@Slf4j
@RestController
@RequestMapping("/asset/categories")
@RequiredArgsConstructor
public class AssetCategoryController {

    private final JdbcTemplate jdbc;

    private static final String CAT_COLUMNS =
        "id, parent_id AS parentId, category_code AS categoryCode, category_name AS categoryName, " +
        "category_type AS categoryType, default_management_mode AS defaultManagementMode, " +
        "depreciation_years AS depreciationYears, unit, sort_order AS sortOrder, remark, " +
        "created_at AS createdAt, updated_at AS updatedAt";

    // ==================== Category Tree ====================

    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> getCategoryTree() {
        List<Map<String, Object>> all = jdbc.queryForList(
            "SELECT " + CAT_COLUMNS + " FROM asset_category WHERE deleted = 0 ORDER BY sort_order, id"
        );

        // Add descriptions and asset counts
        for (Map<String, Object> cat : all) {
            cat.put("categoryTypeDesc", getCategoryTypeDesc(cat.get("categoryType")));
            cat.put("defaultManagementModeDesc", getManagementModeDesc(cat.get("defaultManagementMode")));

            // Count assets in this category
            Long assetCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM asset WHERE category_id = ? AND deleted = 0",
                Long.class, cat.get("id"));
            cat.put("assetCount", assetCount);
        }

        // Build tree
        List<Map<String, Object>> tree = buildTree(all);
        return Result.success(tree);
    }

    // ==================== All Categories (flat) ====================

    @GetMapping
    public Result<List<Map<String, Object>>> getAllCategories() {
        List<Map<String, Object>> categories = jdbc.queryForList(
            "SELECT " + CAT_COLUMNS + " FROM asset_category WHERE deleted = 0 ORDER BY sort_order, id"
        );
        for (Map<String, Object> cat : categories) {
            cat.put("categoryTypeDesc", getCategoryTypeDesc(cat.get("categoryType")));
            cat.put("defaultManagementModeDesc", getManagementModeDesc(cat.get("defaultManagementMode")));
        }
        return Result.success(categories);
    }

    // ==================== Get Category ====================

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getCategory(@PathVariable Long id) {
        Map<String, Object> cat = jdbc.queryForMap(
            "SELECT " + CAT_COLUMNS + " FROM asset_category WHERE id = ? AND deleted = 0", id
        );
        cat.put("categoryTypeDesc", getCategoryTypeDesc(cat.get("categoryType")));
        cat.put("defaultManagementModeDesc", getManagementModeDesc(cat.get("defaultManagementMode")));
        return Result.success(cat);
    }

    // ==================== Create Category ====================

    @PostMapping
    @Transactional
    public Result<Long> createCategory(@RequestBody Map<String, Object> data) {
        long id = IdWorker.getId();

        jdbc.update(
            "INSERT INTO asset_category (id, parent_id, category_code, category_name, category_type, " +
            "default_management_mode, depreciation_years, unit, sort_order, remark, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
            id,
            toLong(data.get("parentId")),
            data.get("categoryCode"),
            data.get("categoryName"),
            data.get("categoryType"),
            data.get("defaultManagementMode"),
            data.get("depreciationYears"),
            data.get("unit"),
            data.getOrDefault("sortOrder", 0),
            data.get("remark")
        );

        return Result.success(id);
    }

    // ==================== Update Category ====================

    @PutMapping("/{id}")
    @Transactional
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        jdbc.update(
            "UPDATE asset_category SET " +
            "parent_id = ?, category_code = ?, category_name = ?, category_type = ?, " +
            "default_management_mode = ?, depreciation_years = ?, unit = ?, sort_order = ?, " +
            "remark = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            toLong(data.get("parentId")),
            data.get("categoryCode"),
            data.get("categoryName"),
            data.get("categoryType"),
            data.get("defaultManagementMode"),
            data.get("depreciationYears"),
            data.get("unit"),
            data.getOrDefault("sortOrder", 0),
            data.get("remark"),
            id
        );

        return Result.success();
    }

    // ==================== Delete Category ====================

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> deleteCategory(@PathVariable Long id) {
        // Check for child categories
        Long childCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset_category WHERE parent_id = ? AND deleted = 0", Long.class, id);
        if (childCount != null && childCount > 0) {
            return Result.error("该分类下有子分类，不能删除");
        }

        // Check for assets using this category
        Long assetCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset WHERE category_id = ? AND deleted = 0", Long.class, id);
        if (assetCount != null && assetCount > 0) {
            return Result.error("该分类下有资产，不能删除");
        }

        jdbc.update("UPDATE asset_category SET deleted = 1, updated_at = NOW() WHERE id = ?", id);
        return Result.success();
    }

    // ==================== Helpers ====================

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildTree(List<Map<String, Object>> all) {
        Map<Object, List<Map<String, Object>>> childrenMap = new HashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (Map<String, Object> item : all) {
            Object parentId = item.get("parentId");
            if (parentId == null) {
                roots.add(item);
            } else {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(item);
            }
        }

        for (Map<String, Object> root : roots) {
            buildChildren(root, childrenMap);
        }

        return roots;
    }

    @SuppressWarnings("unchecked")
    private void buildChildren(Map<String, Object> node, Map<Object, List<Map<String, Object>>> childrenMap) {
        Object id = node.get("id");
        List<Map<String, Object>> children = childrenMap.get(id);
        if (children != null && !children.isEmpty()) {
            node.put("children", children);
            for (Map<String, Object> child : children) {
                buildChildren(child, childrenMap);
            }
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private String getCategoryTypeDesc(Object type) {
        if (type == null) return null;
        int t = ((Number) type).intValue();
        switch (t) {
            case 1: return "固定资产";
            case 2: return "低值易耗品";
            case 3: return "消耗品";
            default: return "未知";
        }
    }

    private String getManagementModeDesc(Object mode) {
        if (mode == null) return null;
        int m = ((Number) mode).intValue();
        switch (m) {
            case 1: return "单品管理";
            case 2: return "批量管理";
            default: return "未知";
        }
    }
}
