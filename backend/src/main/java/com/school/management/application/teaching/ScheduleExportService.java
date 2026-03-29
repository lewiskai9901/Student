package com.school.management.application.teaching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleExportService {
    private final JdbcTemplate jdbcTemplate;

    private static final String[] DAY_NAMES = {"周一", "周二", "周三", "周四", "周五"};
    private static final String[] PERIOD_NAMES = {
        "第1节", "第2节", "第3节", "第4节", "第5节",
        "第6节", "第7节", "第8节", "第9节", "第10节"
    };

    public byte[] exportClassSchedule(Long semesterId, Long classId) throws IOException {
        List<Map<String, Object>> entries = jdbcTemplate.queryForList(
            "SELECT se.weekday, se.start_slot, se.end_slot, " +
            "c.course_name, u.real_name as teacher_name, " +
            "COALESCE(up.name, '') as classroom_name " +
            "FROM schedule_entries se " +
            "LEFT JOIN courses c ON c.id = se.course_id " +
            "LEFT JOIN users u ON u.id = se.teacher_id " +
            "LEFT JOIN universal_places up ON up.id = se.classroom_id " +
            "WHERE se.semester_id = ? AND se.class_id = ? AND se.deleted = 0 " +
            "ORDER BY se.weekday, se.start_slot",
            semesterId, classId);

        // Get class name for the title
        String className = "班级课表";
        try {
            Map<String, Object> cls = jdbcTemplate.queryForMap(
                "SELECT name FROM school_classes WHERE id = ? AND deleted = 0", classId);
            className = cls.get("name") + " 课表";
        } catch (Exception e) {
            log.warn("Failed to get class name for id={}", classId);
        }

        return buildScheduleExcel(className, entries);
    }

    public byte[] exportTeacherSchedule(Long semesterId, Long teacherId) throws IOException {
        List<Map<String, Object>> entries = jdbcTemplate.queryForList(
            "SELECT se.weekday, se.start_slot, se.end_slot, " +
            "c.course_name, " +
            "COALESCE(sc.name, '') as class_name, " +
            "COALESCE(up.name, '') as classroom_name " +
            "FROM schedule_entries se " +
            "LEFT JOIN courses c ON c.id = se.course_id " +
            "LEFT JOIN school_classes sc ON sc.id = se.class_id " +
            "LEFT JOIN universal_places up ON up.id = se.classroom_id " +
            "WHERE se.semester_id = ? AND se.teacher_id = ? AND se.deleted = 0 " +
            "ORDER BY se.weekday, se.start_slot",
            semesterId, teacherId);

        String teacherName = "教师课表";
        try {
            Map<String, Object> teacher = jdbcTemplate.queryForMap(
                "SELECT real_name FROM users WHERE id = ?", teacherId);
            teacherName = teacher.get("real_name") + " 课表";
        } catch (Exception e) {
            log.warn("Failed to get teacher name for id={}", teacherId);
        }

        return buildScheduleExcel(teacherName, entries);
    }

    private byte[] buildScheduleExcel(String title, List<Map<String, Object>> entries) throws IOException {
        // Build grid: 10 periods x 5 days
        String[][] grid = new String[10][5];
        for (Map<String, Object> entry : entries) {
            int day = ((Number) entry.get("weekday")).intValue();
            int startSlot = ((Number) entry.get("start_slot")).intValue();
            int endSlot = ((Number) entry.get("end_slot")).intValue();

            StringBuilder sb = new StringBuilder();
            sb.append(entry.get("course_name"));
            if (entry.get("teacher_name") != null) sb.append("\n").append(entry.get("teacher_name"));
            if (entry.get("class_name") != null && !entry.get("class_name").toString().isEmpty())
                sb.append("\n").append(entry.get("class_name"));
            if (!entry.get("classroom_name").toString().isEmpty())
                sb.append("\n").append(entry.get("classroom_name"));

            for (int slot = startSlot; slot <= endSlot && slot <= 10; slot++) {
                if (day >= 1 && day <= 5) {
                    grid[slot - 1][day - 1] = sb.toString();
                }
            }
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("课表");

            // Styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setWrapText(true);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);

            CellStyle periodStyle = workbook.createCellStyle();
            Font periodFont = workbook.createFont();
            periodFont.setBold(true);
            periodStyle.setFont(periodFont);
            periodStyle.setAlignment(HorizontalAlignment.CENTER);
            periodStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            periodStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            periodStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            periodStyle.setBorderBottom(BorderStyle.THIN);
            periodStyle.setBorderTop(BorderStyle.THIN);
            periodStyle.setBorderLeft(BorderStyle.THIN);
            periodStyle.setBorderRight(BorderStyle.THIN);

            // Title row
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            // Header row
            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(25);
            Cell cornerCell = headerRow.createCell(0);
            cornerCell.setCellValue("节次");
            cornerCell.setCellStyle(headerStyle);
            for (int i = 0; i < 5; i++) {
                Cell cell = headerRow.createCell(i + 1);
                cell.setCellValue(DAY_NAMES[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int period = 0; period < 10; period++) {
                Row row = sheet.createRow(period + 2);
                row.setHeightInPoints(50);

                Cell periodCell = row.createCell(0);
                periodCell.setCellValue(PERIOD_NAMES[period]);
                periodCell.setCellStyle(periodStyle);

                for (int day = 0; day < 5; day++) {
                    Cell cell = row.createCell(day + 1);
                    cell.setCellValue(grid[period][day] != null ? grid[period][day] : "");
                    cell.setCellStyle(cellStyle);
                }
            }

            // Column widths
            sheet.setColumnWidth(0, 3000);
            for (int i = 1; i <= 5; i++) {
                sheet.setColumnWidth(i, 5500);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
