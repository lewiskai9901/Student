package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetCategoryApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Asset Category REST Controller.
 *
 * <p>M2 (2026-05-20): 9 处直 jdbc 已下沉到 AssetCategoryApplicationService.
 * Controller 仅保留 HTTP 绑定 + tree-building / enum desc 表现层 helpers.
 */
@Slf4j
@RestController
@RequestMapping("/asset/categories")
@RequiredArgsConstructor
public class AssetCategoryController {

    private final AssetCategoryApplicationService categoryService;

    // ==================== Category Tree ====================

    @GetMapping("/tree")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<List<Map<String, Object>>> getCategoryTree() {
        List<Map<String, Object>> all = categoryService.listAll();
        for (Map<String, Object> cat : all) {
            cat.put("categoryTypeDesc", getCategoryTypeDesc(cat.get("categoryType")));
            cat.put("defaultManagementModeDesc", getManagementModeDesc(cat.get("defaultManagementMode")));
            cat.put("assetCount", categoryService.countAssetsInCategory(((Number) cat.get("id")).longValue()));
        }
        return Result.success(buildTree(all));
    }

    // ==================== All Categories (flat) ====================

    @GetMapping
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<List<Map<String, Object>>> getAllCategories() {
        List<Map<String, Object>> categories = categoryService.listAll();
        for (Map<String, Object> cat : categories) {
            cat.put("categoryTypeDesc", getCategoryTypeDesc(cat.get("categoryType")));
            cat.put("defaultManagementModeDesc", getManagementModeDesc(cat.get("defaultManagementMode")));
        }
        return Result.success(categories);
    }

    // ==================== Get Category ====================

    @GetMapping("/{id}")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<Map<String, Object>> getCategory(@PathVariable Long id) {
        Map<String, Object> cat = categoryService.findById(id);
        if (cat == null) return Result.error("分类不存在");
        cat.put("categoryTypeDesc", getCategoryTypeDesc(cat.get("categoryType")));
        cat.put("defaultManagementModeDesc", getManagementModeDesc(cat.get("defaultManagementMode")));
        return Result.success(cat);
    }

    // ==================== Create Category ====================

    @PostMapping
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Long> createCategory(@RequestBody Map<String, Object> data) {
        return Result.success(categoryService.create(data));
    }

    // ==================== Update Category ====================

    @PutMapping("/{id}")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        categoryService.update(id, data);
        return Result.success();
    }

    // ==================== Delete Category ====================

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        if (categoryService.countChildren(id) > 0) {
            return Result.error("该分类下有子分类，不能删除");
        }
        if (categoryService.countAssetsInCategory(id) > 0) {
            return Result.error("该分类下有资产，不能删除");
        }
        categoryService.softDelete(id);
        return Result.success();
    }

    // ==================== Presentation Helpers ====================

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
