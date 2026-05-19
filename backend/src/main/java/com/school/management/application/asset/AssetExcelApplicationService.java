package com.school.management.application.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 资产 Excel 导入/导出应用服务 (M2, 2026-05-20).
 *
 * <p>抽 AssetExcelController 内 4 处直 jdbc — 分类查询 / 编码计数 /
 * 资产 INSERT / 导出列表. Excel 解析 / SVG / Cell helpers 留在 controller (表现层).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetExcelApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final AssetCodeApplicationService assetCodeService;

    /** 按 category_code 查 category id; 不存在返 null. */
    public Long findCategoryIdByCode(String categoryCode) {
        try {
            Map<String, Object> cat = jdbcTemplate.queryForMap(
                "SELECT id FROM asset_category WHERE category_code = ? AND deleted = 0",
                categoryCode);
            return ((Number) cat.get("id")).longValue();
        } catch (Exception e) {
            return null;
        }
    }

    /** 用既有编码序号 + 1 生成新编码. */
    public String generateNextAssetCode(String prefix) {
        int next = assetCodeService.countExistingAssetCodes(prefix) + 1;
        return prefix + "-" + String.format("%04d", next);
    }

    /** INSERT 一条资产记录, 返回生成的 id. */
    public long insertAsset(String assetCode, String assetName, Long categoryId,
                            String brand, String model, String unit, int quantity,
                            BigDecimal originalValue, LocalDate purchaseDate,
                            LocalDate warrantyDate, String supplier, String locationType,
                            String locationName, String responsibleUserName, String remark) {
        long id = IdWorker.getId();
        jdbcTemplate.update(
            "INSERT INTO asset (id, asset_code, asset_name, category_id, brand, model, " +
            "unit, quantity, original_value, purchase_date, warranty_date, supplier, " +
            "status, location_type, location_name, responsible_user_name, remark, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, 0)",
            id, assetCode, assetName, categoryId, brand, model, unit, quantity,
            originalValue, purchaseDate, warrantyDate, supplier,
            locationType, locationName, responsibleUserName, remark);
        return id;
    }

    /** 导出查询 — 按 categoryId/status/keyword 过滤. */
    public List<Map<String, Object>> queryAssetsForExport(Long categoryId, Integer status, String keyword) {
        StringBuilder where = new StringBuilder(" WHERE a.deleted = 0");
        List<Object> params = new ArrayList<>();
        if (categoryId != null) {
            where.append(" AND a.category_id = ?");
            params.add(categoryId);
        }
        if (status != null) {
            where.append(" AND a.status = ?");
            params.add(status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (a.asset_code LIKE ? OR a.asset_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        return jdbcTemplate.queryForList(
            "SELECT a.asset_code, a.asset_name, c.category_name, a.brand, a.model, " +
            "a.unit, a.quantity, a.original_value, a.net_value, a.purchase_date, " +
            "a.warranty_date, a.supplier, a.status, a.location_name, " +
            "a.responsible_user_name, a.remark " +
            "FROM asset a LEFT JOIN asset_category c ON a.category_id = c.id" + where +
            " ORDER BY a.asset_code",
            params.toArray());
    }
}
