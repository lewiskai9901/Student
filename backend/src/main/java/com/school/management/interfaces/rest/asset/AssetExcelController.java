package com.school.management.interfaces.rest.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Asset Excel Import/Export REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/assets/excel")
@RequiredArgsConstructor
public class AssetExcelController {

    private final JdbcTemplate jdbc;

    private static final String[] TEMPLATE_HEADERS = {
        "资产名称(*)", "分类编码(*)", "品牌", "型号", "单位(*)", "数量",
        "原值", "购入日期(yyyy-MM-dd)", "保修到期(yyyy-MM-dd)", "供应商",
        "位置类型", "位置名称", "责任人", "备注"
    };

    // ==================== Download Template ====================

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("资产导入模板.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("资产导入");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Write headers
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Write sample row
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("联想笔记本电脑");
            sampleRow.createCell(1).setCellValue("IT-PC");
            sampleRow.createCell(2).setCellValue("联想");
            sampleRow.createCell(3).setCellValue("ThinkPad X1");
            sampleRow.createCell(4).setCellValue("台");
            sampleRow.createCell(5).setCellValue(1);
            sampleRow.createCell(6).setCellValue(8999);
            sampleRow.createCell(7).setCellValue("2026-01-15");
            sampleRow.createCell(8).setCellValue("2029-01-15");
            sampleRow.createCell(9).setCellValue("联想科技");
            sampleRow.createCell(10).setCellValue("office");
            sampleRow.createCell(11).setCellValue("行政办公室");
            sampleRow.createCell(12).setCellValue("张三");
            sampleRow.createCell(13).setCellValue("");

            workbook.write(response.getOutputStream());
        }
    }

    // ==================== Import ====================

    @PostMapping("/import")
    @Transactional
    public Result<Map<String, Object>> importAssets(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }

        int totalCount = 0;
        int successCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int rowNum = 1; rowNum <= lastRow; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                totalCount++;
                try {
                    String assetName = getCellString(row, 0);
                    String categoryCode = getCellString(row, 1);
                    String unit = getCellString(row, 4);

                    if (assetName == null || assetName.isEmpty()) {
                        addError(errors, rowNum + 1, assetName, "资产名称不能为空");
                        continue;
                    }
                    if (unit == null || unit.isEmpty()) {
                        unit = "个";
                    }

                    // Find category
                    Long categoryId = null;
                    if (categoryCode != null && !categoryCode.isEmpty()) {
                        try {
                            Map<String, Object> cat = jdbc.queryForMap(
                                "SELECT id FROM asset_category WHERE category_code = ? AND deleted = 0",
                                categoryCode);
                            categoryId = ((Number) cat.get("id")).longValue();
                        } catch (Exception e) {
                            addError(errors, rowNum + 1, assetName, "分类编码不存在: " + categoryCode);
                            continue;
                        }
                    }

                    // Generate asset code
                    String prefix = categoryCode != null ? categoryCode : "AST";
                    Long count = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM asset WHERE asset_code LIKE ? AND deleted = 0",
                        Long.class, prefix + "-%");
                    String assetCode = prefix + "-" + String.format("%04d", (count != null ? count + 1 : 1));

                    long id = IdWorker.getId();
                    jdbc.update(
                        "INSERT INTO asset (id, asset_code, asset_name, category_id, brand, model, " +
                        "unit, quantity, original_value, purchase_date, warranty_date, supplier, " +
                        "status, location_type, location_name, responsible_user_name, remark, deleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, 0)",
                        id, assetCode, assetName, categoryId,
                        getCellString(row, 2),  // brand
                        getCellString(row, 3),  // model
                        unit,
                        getCellInt(row, 5, 1),  // quantity
                        getCellDecimal(row, 6), // originalValue
                        getCellDate(row, 7),    // purchaseDate
                        getCellDate(row, 8),    // warrantyDate
                        getCellString(row, 9),  // supplier
                        getCellString(row, 10), // locationType
                        getCellString(row, 11), // locationName
                        getCellString(row, 12), // responsibleUserName
                        getCellString(row, 13)  // remark
                    );
                    successCount++;

                } catch (Exception e) {
                    String assetName = getCellString(row, 0);
                    addError(errors, rowNum + 1, assetName, "导入失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Import assets failed", e);
            return Result.error("文件解析失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("failCount", totalCount - successCount);
        result.put("errors", errors);
        return Result.success(result);
    }

    // ==================== Export ====================

    @GetMapping("/export")
    public void exportAssets(
            HttpServletResponse response,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) throws IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("资产列表.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

        // Build query
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

        List<Map<String, Object>> assets = jdbc.queryForList(
            "SELECT a.asset_code, a.asset_name, c.category_name, a.brand, a.model, " +
            "a.unit, a.quantity, a.original_value, a.net_value, a.purchase_date, " +
            "a.warranty_date, a.supplier, a.status, a.location_name, " +
            "a.responsible_user_name, a.remark " +
            "FROM asset a LEFT JOIN asset_category c ON a.category_id = c.id" + where +
            " ORDER BY a.asset_code",
            params.toArray()
        );

        String[] headers = {
            "资产编码", "资产名称", "分类", "品牌", "型号", "单位", "数量",
            "原值", "净值", "购入日期", "保修到期", "供应商", "状态",
            "位置", "责任人", "备注"
        };

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("资产列表");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4500);
            }

            int rowIdx = 1;
            for (Map<String, Object> asset : assets) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(toString(asset.get("asset_code")));
                row.createCell(1).setCellValue(toString(asset.get("asset_name")));
                row.createCell(2).setCellValue(toString(asset.get("category_name")));
                row.createCell(3).setCellValue(toString(asset.get("brand")));
                row.createCell(4).setCellValue(toString(asset.get("model")));
                row.createCell(5).setCellValue(toString(asset.get("unit")));
                row.createCell(6).setCellValue(toInt(asset.get("quantity")));
                row.createCell(7).setCellValue(toDouble(asset.get("original_value")));
                row.createCell(8).setCellValue(toDouble(asset.get("net_value")));
                row.createCell(9).setCellValue(toString(asset.get("purchase_date")));
                row.createCell(10).setCellValue(toString(asset.get("warranty_date")));
                row.createCell(11).setCellValue(toString(asset.get("supplier")));
                row.createCell(12).setCellValue(getStatusDesc(asset.get("status")));
                row.createCell(13).setCellValue(toString(asset.get("location_name")));
                row.createCell(14).setCellValue(toString(asset.get("responsible_user_name")));
                row.createCell(15).setCellValue(toString(asset.get("remark")));
            }

            workbook.write(response.getOutputStream());
        }
    }

    // ==================== Helpers ====================

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        String val = cell.getStringCellValue();
        return val != null && !val.trim().isEmpty() ? val.trim() : null;
    }

    private int getCellInt(Row row, int col, int defaultVal) {
        Cell cell = row.getCell(col);
        if (cell == null) return defaultVal;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            return Integer.parseInt(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private BigDecimal getCellDecimal(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            String val = cell.getStringCellValue().trim();
            return val.isEmpty() ? null : new BigDecimal(val);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate getCellDate(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String val = cell.getStringCellValue().trim();
            return val.isEmpty() ? null : LocalDate.parse(val);
        } catch (Exception e) {
            return null;
        }
    }

    private void addError(List<Map<String, Object>> errors, int rowNum, String assetName, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("rowNum", rowNum);
        error.put("assetName", assetName);
        error.put("errorMessage", message);
        errors.add(error);
    }

    private String toString(Object val) {
        return val != null ? val.toString() : "";
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        return ((Number) val).intValue();
    }

    private double toDouble(Object val) {
        if (val == null) return 0;
        return ((Number) val).doubleValue();
    }

    private String getStatusDesc(Object status) {
        if (status == null) return "";
        int s = ((Number) status).intValue();
        switch (s) {
            case 1: return "在用";
            case 2: return "闲置";
            case 3: return "维修中";
            case 4: return "已报废";
            default: return "未知";
        }
    }
}
