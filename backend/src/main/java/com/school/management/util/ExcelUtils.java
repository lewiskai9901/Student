package com.school.management.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel工具类
 */
public class ExcelUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出Excel
     *
     * @param headers 表头
     * @param data 数据
     * @param sheetName 工作表名称
     * @return Excel字节数组
     */
    public static byte[] exportExcel(String[] headers, List<Object[]> data, String sheetName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);

            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 创建表头行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // 创建数据行
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Object[] rowData = data.get(i);
                for (int j = 0; j < rowData.length; j++) {
                    Cell cell = row.createCell(j);
                    setCellValue(cell, rowData[j]);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 从Excel导入数据
     *
     * @param inputStream 输入流
     * @param startRow 起始行(0-based, 通常表头在第0行,数据从第1行开始)
     * @return 数据列表
     */
    public static List<List<Object>> importExcel(InputStream inputStream, int startRow) throws IOException {
        List<List<Object>> result = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int i = startRow; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                List<Object> rowData = new ArrayList<>();
                int lastCellNum = row.getLastCellNum();

                for (int j = 0; j < lastCellNum; j++) {
                    Cell cell = row.getCell(j);
                    rowData.add(getCellValue(cell));
                }

                // 跳过空行
                if (rowData.stream().allMatch(obj -> obj == null || obj.toString().trim().isEmpty())) {
                    continue;
                }

                result.add(rowData);
            }
        }

        return result;
    }

    /**
     * 创建表头样式
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        return style;
    }

    /**
     * 设置单元格值
     */
    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DATE_FORMATTER));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 获取单元格值
     */
    private static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    /**
     * 导出带有下拉选择的Excel模板
     *
     * @param headers 表头
     * @param data 示例数据
     * @param sheetName 工作表名称
     * @param dropdownOptions 下拉选项配置 (key: 列索引, value: 下拉选项数组)
     * @return Excel字节数组
     */
    public static byte[] exportTemplateWithDropdown(String[] headers, List<Object[]> data,
            String sheetName, Map<Integer, String[]> dropdownOptions) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet(sheetName);

            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 创建必填字段样式（红色字体）
            CellStyle requiredHeaderStyle = createRequiredHeaderStyle(workbook);

            // 创建表头行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                // 必填字段（带*号）使用红色样式
                if (headers[i].endsWith("*")) {
                    cell.setCellStyle(requiredHeaderStyle);
                } else {
                    cell.setCellStyle(headerStyle);
                }
                sheet.setColumnWidth(i, 4500);
            }

            // 创建数据行
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Object[] rowData = data.get(i);
                for (int j = 0; j < rowData.length; j++) {
                    Cell cell = row.createCell(j);
                    setCellValue(cell, rowData[j]);
                }
            }

            // 添加下拉选项
            if (dropdownOptions != null && !dropdownOptions.isEmpty()) {
                XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);
                for (Map.Entry<Integer, String[]> entry : dropdownOptions.entrySet()) {
                    int colIndex = entry.getKey();
                    String[] options = entry.getValue();

                    // 设置下拉选项范围（从第2行到第1000行）
                    CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, colIndex, colIndex);
                    XSSFDataValidationConstraint constraint = (XSSFDataValidationConstraint)
                            dvHelper.createExplicitListConstraint(options);
                    XSSFDataValidation validation = (XSSFDataValidation)
                            dvHelper.createValidation(constraint, addressList);

                    // 设置错误提示
                    validation.setShowErrorBox(true);
                    validation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
                    validation.createErrorBox("输入错误", "请从下拉列表中选择");

                    sheet.addValidationData(validation);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 创建必填字段表头样式（红色字体）
     */
    private static CellStyle createRequiredHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);

        return style;
    }
}
