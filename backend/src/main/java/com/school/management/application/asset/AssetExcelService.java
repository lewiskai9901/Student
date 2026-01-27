package com.school.management.application.asset;

import com.school.management.application.asset.command.CreateAssetCommand;
import com.school.management.application.asset.query.AssetDTO;
import com.school.management.application.asset.query.AssetQueryCriteria;
import com.school.management.domain.asset.model.entity.AssetCategory;
import com.school.management.domain.asset.repository.AssetCategoryRepository;
import com.school.management.interfaces.rest.asset.dto.ImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产Excel导入导出服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetExcelService {

    private final FixedAssetApplicationService assetService;
    private final AssetCategoryRepository categoryRepository;

    // Excel模板列定义
    private static final String[] TEMPLATE_HEADERS = {
            "资产名称*", "分类编码*", "品牌", "型号", "数量", "单价",
            "购置日期", "保修期至", "供应商", "位置类型", "位置名称", "责任人", "备注"
    };

    // 导出列定义
    private static final String[] EXPORT_HEADERS = {
            "资产编号", "资产名称", "分类名称", "分类编码", "品牌", "型号",
            "数量", "单位", "原值", "净值", "购置日期", "保修期至", "供应商",
            "状态", "位置类型", "位置名称", "责任人", "备注", "创建时间"
    };

    /**
     * 下载导入模板
     */
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("资产导入模板");

            // 创建标题行样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 15 * 256);
            }

            // 添加示例数据行
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("联想笔记本电脑");
            exampleRow.createCell(1).setCellValue("TEACH-COMP");
            exampleRow.createCell(2).setCellValue("联想");
            exampleRow.createCell(3).setCellValue("ThinkPad E14");
            exampleRow.createCell(4).setCellValue(1);
            exampleRow.createCell(5).setCellValue(5000);
            exampleRow.createCell(6).setCellValue("2026-01-25");
            exampleRow.createCell(7).setCellValue("2029-01-25");
            exampleRow.createCell(8).setCellValue("联想官方商城");
            exampleRow.createCell(9).setCellValue("warehouse");
            exampleRow.createCell(10).setCellValue("总务处仓库");
            exampleRow.createCell(11).setCellValue("张三");
            exampleRow.createCell(12).setCellValue("示例备注");

            // 创建说明sheet
            Sheet helpSheet = workbook.createSheet("填写说明");
            createHelpSheet(helpSheet, workbook);

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("资产导入模板.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
            }
        }
    }

    /**
     * 导入资产
     */
    @Transactional
    public ImportResult importAssets(MultipartFile file, Long operatorId, String operatorName) throws IOException {
        ImportResult result = ImportResult.builder()
                .totalCount(0)
                .successCount(0)
                .failCount(0)
                .errors(new ArrayList<>())
                .build();

        // 预加载分类映射
        Map<String, AssetCategory> categoryMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(
                        AssetCategory::getCategoryCode,
                        c -> c,
                        (a, b) -> a
                ));

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            int rowCount = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {  // 从第2行开始（跳过标题）
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                rowCount++;
                String assetName = getCellStringValue(row.getCell(0));
                String categoryCode = getCellStringValue(row.getCell(1));

                try {
                    // 验证必填字段
                    if (assetName == null || assetName.trim().isEmpty()) {
                        result.addError(i + 1, assetName, "资产名称不能为空");
                        continue;
                    }
                    if (categoryCode == null || categoryCode.trim().isEmpty()) {
                        result.addError(i + 1, assetName, "分类编码不能为空");
                        continue;
                    }

                    // 验证分类
                    AssetCategory category = categoryMap.get(categoryCode.trim());
                    if (category == null) {
                        result.addError(i + 1, assetName, "分类编码 \"" + categoryCode + "\" 不存在");
                        continue;
                    }

                    // 解析其他字段
                    String brand = getCellStringValue(row.getCell(2));
                    String model = getCellStringValue(row.getCell(3));
                    Integer quantity = getCellIntValue(row.getCell(4));
                    BigDecimal price = getCellDecimalValue(row.getCell(5));
                    LocalDate purchaseDate = getCellDateValue(row.getCell(6));
                    LocalDate warrantyDate = getCellDateValue(row.getCell(7));
                    String supplier = getCellStringValue(row.getCell(8));
                    String locationType = getCellStringValue(row.getCell(9));
                    String locationName = getCellStringValue(row.getCell(10));
                    String responsibleUserName = getCellStringValue(row.getCell(11));
                    String remark = getCellStringValue(row.getCell(12));

                    // 验证数量
                    if (quantity != null && quantity <= 0) {
                        result.addError(i + 1, assetName, "数量必须大于0");
                        continue;
                    }

                    // 创建资产
                    CreateAssetCommand command = CreateAssetCommand.builder()
                            .assetName(assetName.trim())
                            .categoryId(category.getId())
                            .brand(brand)
                            .model(model)
                            .unit(category.getUnit() != null ? category.getUnit() : "个")
                            .quantity(quantity != null ? quantity : 1)
                            .originalValue(price)
                            .netValue(price)
                            .purchaseDate(purchaseDate)
                            .warrantyDate(warrantyDate)
                            .supplier(supplier)
                            .locationType(locationType)
                            .locationName(locationName)
                            .responsibleUserName(responsibleUserName)
                            .remark(remark)
                            .operatorId(operatorId)
                            .operatorName(operatorName)
                            .build();

                    assetService.createAsset(command);
                    result.setSuccessCount(result.getSuccessCount() + 1);

                } catch (Exception e) {
                    log.error("Import row {} failed: {}", i + 1, e.getMessage());
                    result.addError(i + 1, assetName, "导入失败: " + e.getMessage());
                }
            }

            result.setTotalCount(rowCount);
            result.setFailCount(result.getErrors().size());
        }

        log.info("Import completed: total={}, success={}, fail={}",
                result.getTotalCount(), result.getSuccessCount(), result.getFailCount());

        return result;
    }

    /**
     * 导出资产列表
     */
    public void exportAssets(AssetQueryCriteria criteria, HttpServletResponse response) throws IOException {
        // 获取所有符合条件的资产（不分页）
        criteria.setPageNum(1);
        criteria.setPageSize(10000);
        var page = assetService.queryAssets(criteria);
        List<AssetDTO> assets = page.getRecords();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("资产列表");

            // 创建标题行样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 创建日期格式
            CellStyle dateStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            dateStyle.setDataFormat(dataFormat.getFormat("yyyy-MM-dd"));

            // 创建金额格式
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            int rowNum = 1;
            for (AssetDTO asset : assets) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(asset.getAssetCode());
                row.createCell(1).setCellValue(asset.getAssetName());
                row.createCell(2).setCellValue(asset.getCategoryName() != null ? asset.getCategoryName() : "");
                row.createCell(3).setCellValue(asset.getCategoryCode() != null ? asset.getCategoryCode() : "");
                row.createCell(4).setCellValue(asset.getBrand() != null ? asset.getBrand() : "");
                row.createCell(5).setCellValue(asset.getModel() != null ? asset.getModel() : "");
                row.createCell(6).setCellValue(asset.getQuantity() != null ? asset.getQuantity() : 1);
                row.createCell(7).setCellValue(asset.getUnit() != null ? asset.getUnit() : "");

                Cell priceCell = row.createCell(8);
                if (asset.getOriginalValue() != null) {
                    priceCell.setCellValue(asset.getOriginalValue().doubleValue());
                    priceCell.setCellStyle(moneyStyle);
                }

                Cell netValueCell = row.createCell(9);
                if (asset.getNetValue() != null) {
                    netValueCell.setCellValue(asset.getNetValue().doubleValue());
                    netValueCell.setCellStyle(moneyStyle);
                }

                row.createCell(10).setCellValue(asset.getPurchaseDate() != null ? asset.getPurchaseDate().toString() : "");
                row.createCell(11).setCellValue(asset.getWarrantyDate() != null ? asset.getWarrantyDate().toString() : "");
                row.createCell(12).setCellValue(asset.getSupplier() != null ? asset.getSupplier() : "");
                row.createCell(13).setCellValue(asset.getStatusDesc() != null ? asset.getStatusDesc() : "");
                row.createCell(14).setCellValue(asset.getLocationTypeDesc() != null ? asset.getLocationTypeDesc() : "");
                row.createCell(15).setCellValue(asset.getLocationName() != null ? asset.getLocationName() : "");
                row.createCell(16).setCellValue(asset.getResponsibleUserName() != null ? asset.getResponsibleUserName() : "");
                row.createCell(17).setCellValue(asset.getRemark() != null ? asset.getRemark() : "");
                row.createCell(18).setCellValue(asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : "");
            }

            // 自动调整列宽
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                // 设置最小宽度
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
                // 设置最大宽度
                if (sheet.getColumnWidth(i) > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("资产列表_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
            }
        }
    }

    // ============ 辅助方法 ============

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    private void createHelpSheet(Sheet sheet, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[][] helpContent = {
                {"字段名称", "是否必填", "说明"},
                {"资产名称", "是", "资产的名称，最多200字符"},
                {"分类编码", "是", "资产分类的编码，需与系统中的分类编码一致"},
                {"品牌", "否", "资产品牌"},
                {"型号", "否", "资产型号规格"},
                {"数量", "否", "默认为1"},
                {"单价", "否", "资产单价（元）"},
                {"购置日期", "否", "格式：yyyy-MM-dd，如 2026-01-25"},
                {"保修期至", "否", "格式：yyyy-MM-dd"},
                {"供应商", "否", "供应商名称"},
                {"位置类型", "否", "classroom/dormitory/office/warehouse/other"},
                {"位置名称", "否", "具体位置名称"},
                {"责任人", "否", "责任人姓名"},
                {"备注", "否", "备注信息"}
        };

        for (int i = 0; i < helpContent.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < helpContent[i].length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(helpContent[i][j]);
                if (i == 0) {
                    cell.setCellStyle(headerStyle);
                }
            }
        }

        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 10 * 256);
        sheet.setColumnWidth(2, 50 * 256);
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                // 避免科学计数法
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value)) {
                    return String.valueOf((long) value);
                }
                return String.valueOf(value);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    private Integer getCellIntValue(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return null;
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private BigDecimal getCellDecimalValue(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return null;
                return new BigDecimal(value);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private LocalDate getCellDateValue(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return null;
                return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (DateTimeParseException e) {
            return null;
        }
        return null;
    }
}
