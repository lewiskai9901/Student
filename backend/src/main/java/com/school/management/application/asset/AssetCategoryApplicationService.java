package com.school.management.application.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 资产分类应用服务 (M2, 2026-05-20).
 * AssetCategoryController 9 处直 jdbc 全部下沉到此.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetCategoryApplicationService {

    private final JdbcTemplate jdbcTemplate;

    private static final String CAT_COLUMNS =
        "id, parent_id AS parentId, category_code AS categoryCode, category_name AS categoryName, " +
        "category_type AS categoryType, default_management_mode AS defaultManagementMode, " +
        "depreciation_years AS depreciationYears, unit, sort_order AS sortOrder, remark, " +
        "created_at AS createdAt, updated_at AS updatedAt";

    public List<Map<String, Object>> listAll() {
        return jdbcTemplate.queryForList(
            "SELECT " + CAT_COLUMNS + " FROM asset_category WHERE deleted = 0 ORDER BY sort_order, id");
    }

    /** 取一条分类; 不存在返 null. */
    public Map<String, Object> findById(Long id) {
        try {
            return jdbcTemplate.queryForMap(
                "SELECT " + CAT_COLUMNS + " FROM asset_category WHERE id = ? AND deleted = 0", id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public long countAssetsInCategory(Long categoryId) {
        Long c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset WHERE category_id = ? AND deleted = 0", Long.class, categoryId);
        return c == null ? 0 : c;
    }

    public long countChildren(Long parentId) {
        Long c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_category WHERE parent_id = ? AND deleted = 0",
            Long.class, parentId);
        return c == null ? 0 : c;
    }

    @Transactional
    public long create(Map<String, Object> data) {
        long id = IdWorker.getId();
        jdbcTemplate.update(
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
            data.get("remark"));
        return id;
    }

    @Transactional
    public void update(Long id, Map<String, Object> data) {
        jdbcTemplate.update(
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
            id);
    }

    @Transactional
    public void softDelete(Long id) {
        jdbcTemplate.update(
            "UPDATE asset_category SET deleted = 1, updated_at = NOW() WHERE id = ?", id);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}
