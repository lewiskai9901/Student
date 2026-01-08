package com.school.management.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.school.management.dto.analysis.AnalysisReportDTO;
import com.school.management.dto.analysis.AnalysisReportDTO.*;
import com.school.management.exception.BusinessException;
import com.school.management.service.AnalysisExportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * 分析报告导出服务实现类
 *
 * @author Claude
 * @since 2025-12-05
 */
@Slf4j
@Service
public class AnalysisExportServiceImpl implements AnalysisExportService {

    @Override
    public byte[] exportToExcel(AnalysisReportDTO report) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            // 1. 创建概览Sheet
            createOverviewSheet(workbook, report, titleStyle, headerStyle, dataStyle, numberStyle);

            // 2. 创建频次统计Sheet
            if (report.getData().getFrequencyStats() != null) {
                createFrequencySheet(workbook, report.getData().getFrequencyStats(), headerStyle, dataStyle, numberStyle);
            }

            // 3. 创建班级排名Sheet
            if (report.getData().getClassRanking() != null && !report.getData().getClassRanking().isEmpty()) {
                createClassRankingSheet(workbook, report.getData().getClassRanking(), headerStyle, dataStyle, numberStyle);
            }

            // 4. 创建学生排名Sheet
            if (report.getData().getStudentRanking() != null && !report.getData().getStudentRanking().isEmpty()) {
                createStudentRankingSheet(workbook, report.getData().getStudentRanking(), headerStyle, dataStyle, numberStyle);
            }

            // 5. 创建趋势数据Sheet
            if (report.getData().getTrendAnalysis() != null) {
                createTrendSheet(workbook, report.getData().getTrendAnalysis(), headerStyle, dataStyle, numberStyle);
            }

            // 6. 创建分布数据Sheet
            if (report.getData().getDistribution() != null && !report.getData().getDistribution().isEmpty()) {
                createDistributionSheet(workbook, report.getData().getDistribution(), headerStyle, dataStyle, numberStyle);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("导出Excel失败", e);
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportToPdf(AnalysisReportDTO report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            // 创建中文字体
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 18, Font.BOLD);
            Font headerFont = new Font(bfChinese, 12, Font.BOLD);
            Font normalFont = new Font(bfChinese, 10, Font.NORMAL);

            // 标题
            Paragraph title = new Paragraph(report.getConfigName() + " - 分析报告", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 基本信息
            document.add(new Paragraph("报告信息", headerFont));
            document.add(new Paragraph("分析目标: " + report.getTargetName(), normalFont));
            document.add(new Paragraph("目标类型: " + getTargetTypeName(report.getTargetType()), normalFont));
            document.add(new Paragraph("时间范围: " + report.getStartDate() + " 至 " + report.getEndDate(), normalFont));
            document.add(new Paragraph("生成时间: " + report.getGeneratedAt(), normalFont));
            document.add(new Paragraph(" ", normalFont));

            // 统计概览
            if (report.getData().getFrequencyStats() != null || report.getData().getScoreStats() != null) {
                document.add(new Paragraph("统计概览", headerFont));

                FrequencyStats freqStats = report.getData().getFrequencyStats();
                ScoreStats scoreStats = report.getData().getScoreStats();

                if (freqStats != null) {
                    document.add(new Paragraph("检查次数: " + freqStats.getTotalChecks(), normalFont));
                    document.add(new Paragraph("扣分次数: " + freqStats.getTotalDeductions(), normalFont));
                    document.add(new Paragraph("涉及班级数: " + freqStats.getClassCount(), normalFont));
                }

                if (scoreStats != null) {
                    document.add(new Paragraph("总扣分: " + formatDecimal(scoreStats.getTotalDeduction()), normalFont));
                    document.add(new Paragraph("平均扣分: " + formatDecimal(scoreStats.getAvgDeduction()), normalFont));
                }
                document.add(new Paragraph(" ", normalFont));
            }

            // 班级排名表格
            if (report.getData().getClassRanking() != null && !report.getData().getClassRanking().isEmpty()) {
                document.add(new Paragraph("班级排名", headerFont));
                PdfPTable table = createPdfTable(4, "排名", "班级名称", "扣分次数", "总扣分");

                for (RankingItem item : report.getData().getClassRanking()) {
                    addPdfTableRow(table, normalFont,
                            String.valueOf(item.getRank()),
                            item.getName(),
                            String.valueOf(item.getDeductionCount()),
                            formatDecimal(item.getTotalDeduction()));
                }
                document.add(table);
                document.add(new Paragraph(" ", normalFont));
            }

            // 学生排名表格
            if (report.getData().getStudentRanking() != null && !report.getData().getStudentRanking().isEmpty()) {
                document.add(new Paragraph("学生排名", headerFont));
                PdfPTable table = createPdfTable(6, "排名", "学号", "姓名", "班级", "扣分次数", "总扣分");

                for (StudentRankingItem item : report.getData().getStudentRanking()) {
                    addPdfTableRow(table, normalFont,
                            String.valueOf(item.getRank()),
                            item.getStudentNo(),
                            item.getStudentName(),
                            item.getClassName(),
                            String.valueOf(item.getDeductionCount()),
                            formatDecimal(item.getTotalDeduction()));
                }
                document.add(table);
            }

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("导出PDF失败", e);
            throw new BusinessException("导出PDF失败: " + e.getMessage());
        }
    }

    private PdfPTable createPdfTable(int columns, String... headers) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        try {
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font headerFont = new Font(bfChinese, 10, Font.BOLD, Color.WHITE);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(64, 158, 255));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }
        } catch (Exception e) {
            log.error("创建PDF表头失败", e);
        }

        return table;
    }

    private void addPdfTableRow(PdfPTable table, Font font, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    // ==================== 创建各Sheet ====================

    private void createOverviewSheet(Workbook workbook, AnalysisReportDTO report,
            CellStyle titleStyle, CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("报告概览");

        int rowNum = 0;

        // 标题
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(report.getConfigName() + " - 分析报告");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        rowNum++; // 空行

        // 基本信息
        createInfoRow(sheet, rowNum++, "分析目标", report.getTargetName(), dataStyle);
        createInfoRow(sheet, rowNum++, "目标类型", getTargetTypeName(report.getTargetType()), dataStyle);
        createInfoRow(sheet, rowNum++, "时间范围", report.getStartDate() + " 至 " + report.getEndDate(), dataStyle);
        createInfoRow(sheet, rowNum++, "生成时间", report.getGeneratedAt().toString(), dataStyle);

        rowNum++; // 空行

        // 统计概览
        if (report.getData().getFrequencyStats() != null || report.getData().getScoreStats() != null) {
            Row statHeaderRow = sheet.createRow(rowNum++);
            Cell statHeaderCell = statHeaderRow.createCell(0);
            statHeaderCell.setCellValue("统计概览");
            statHeaderCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

            FrequencyStats freqStats = report.getData().getFrequencyStats();
            ScoreStats scoreStats = report.getData().getScoreStats();

            if (freqStats != null) {
                createInfoRow(sheet, rowNum++, "检查次数", String.valueOf(freqStats.getTotalChecks()), dataStyle);
                createInfoRow(sheet, rowNum++, "扣分次数", String.valueOf(freqStats.getTotalDeductions()), dataStyle);
                createInfoRow(sheet, rowNum++, "涉及班级数", String.valueOf(freqStats.getClassCount()), dataStyle);
            }

            if (scoreStats != null) {
                createInfoRow(sheet, rowNum++, "总扣分", formatDecimal(scoreStats.getTotalDeduction()), dataStyle);
                createInfoRow(sheet, rowNum++, "平均扣分", formatDecimal(scoreStats.getAvgDeduction()), dataStyle);
                createInfoRow(sheet, rowNum++, "最高单次扣分", formatDecimal(scoreStats.getMaxDeduction()), dataStyle);
                createInfoRow(sheet, rowNum++, "最低单次扣分", formatDecimal(scoreStats.getMinDeduction()), dataStyle);
            }
        }

        // 调整列宽
        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 30 * 256);
    }

    private void createFrequencySheet(Workbook workbook, FrequencyStats stats,
            CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        if (stats.getItemFrequencies() == null || stats.getItemFrequencies().isEmpty()) {
            return;
        }

        Sheet sheet = workbook.createSheet("频次统计");
        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"项目名称", "次数", "占比(%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (FrequencyItem item : stats.getItemFrequencies()) {
            Row row = sheet.createRow(rowNum++);

            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(item.getName());
            nameCell.setCellStyle(dataStyle);

            Cell countCell = row.createCell(1);
            countCell.setCellValue(item.getCount());
            countCell.setCellStyle(numberStyle);

            Cell percentCell = row.createCell(2);
            percentCell.setCellValue(item.getPercentage() != null ? item.getPercentage().doubleValue() : 0);
            percentCell.setCellStyle(numberStyle);
        }

        // 调整列宽
        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 12 * 256);
    }

    private void createClassRankingSheet(Workbook workbook, List<RankingItem> ranking,
            CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("班级排名");
        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"排名", "班级名称", "扣分次数", "总扣分"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (RankingItem item : ranking) {
            Row row = sheet.createRow(rowNum++);

            createNumberCell(row, 0, item.getRank(), numberStyle);
            createTextCell(row, 1, item.getName(), dataStyle);
            createNumberCell(row, 2, item.getDeductionCount(), numberStyle);
            createDecimalCell(row, 3, item.getTotalDeduction(), numberStyle);
        }

        // 调整列宽
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 25 * 256);
        sheet.setColumnWidth(2, 12 * 256);
        sheet.setColumnWidth(3, 12 * 256);
    }

    private void createStudentRankingSheet(Workbook workbook, List<StudentRankingItem> ranking,
            CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("学生排名");
        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"排名", "学号", "姓名", "班级", "扣分次数", "总扣分"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (StudentRankingItem item : ranking) {
            Row row = sheet.createRow(rowNum++);

            createNumberCell(row, 0, item.getRank(), numberStyle);
            createTextCell(row, 1, item.getStudentNo(), dataStyle);
            createTextCell(row, 2, item.getStudentName(), dataStyle);
            createTextCell(row, 3, item.getClassName(), dataStyle);
            createNumberCell(row, 4, item.getDeductionCount(), numberStyle);
            createDecimalCell(row, 5, item.getTotalDeduction(), numberStyle);
        }

        // 调整列宽
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.setColumnWidth(2, 12 * 256);
        sheet.setColumnWidth(3, 20 * 256);
        sheet.setColumnWidth(4, 12 * 256);
        sheet.setColumnWidth(5, 12 * 256);
    }

    private void createTrendSheet(Workbook workbook, TrendAnalysis trend,
            CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        if (trend.getDates() == null || trend.getDates().isEmpty()) {
            return;
        }

        Sheet sheet = workbook.createSheet("趋势数据");
        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"日期", "检查次数", "扣分"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (int i = 0; i < trend.getDates().size(); i++) {
            Row row = sheet.createRow(rowNum++);

            createTextCell(row, 0, trend.getDates().get(i), dataStyle);

            if (trend.getCheckCountTrend() != null && i < trend.getCheckCountTrend().size()) {
                createNumberCell(row, 1, trend.getCheckCountTrend().get(i), numberStyle);
            }

            if (trend.getDeductionTrend() != null && i < trend.getDeductionTrend().size()) {
                createDecimalCell(row, 2, trend.getDeductionTrend().get(i), numberStyle);
            }
        }

        // 调整列宽
        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 12 * 256);
    }

    private void createDistributionSheet(Workbook workbook, List<DistributionItem> distribution,
            CellStyle headerStyle, CellStyle dataStyle, CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("问题分布");
        int rowNum = 0;

        // 表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"项目", "扣分值", "占比(%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        for (DistributionItem item : distribution) {
            Row row = sheet.createRow(rowNum++);

            createTextCell(row, 0, item.getName(), dataStyle);
            createDecimalCell(row, 1, item.getValue(), numberStyle);
            createDecimalCell(row, 2, item.getPercentage(), numberStyle);
        }

        // 调整列宽
        sheet.setColumnWidth(0, 25 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 12 * 256);
    }

    // ==================== 工具方法 ====================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.RIGHT);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private void createInfoRow(Sheet sheet, int rowNum, String label, String value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label + ":");
        labelCell.setCellStyle(style);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(style);
    }

    private void createTextCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createNumberCell(Row row, int col, Integer value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : 0);
        cell.setCellStyle(style);
    }

    private void createDecimalCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value.doubleValue() : 0);
        cell.setCellStyle(style);
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) return "0";
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }

    private String getTargetTypeName(String targetType) {
        if (targetType == null) return "";
        switch (targetType) {
            case "TEMPLATE": return "按检查模板";
            case "CATEGORY": return "按检查类别";
            case "DEDUCTION_ITEM": return "按扣分项";
            case "SINGLE_CHECK": return "按单次检查";
            case "ORGANIZATION": return "按组织";
            default: return targetType;
        }
    }
}
