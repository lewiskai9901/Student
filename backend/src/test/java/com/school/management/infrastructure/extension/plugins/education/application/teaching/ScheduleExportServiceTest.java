package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScheduleExportService 课表 Excel 导出服务单测.
 *
 * 用 Mockito 隔离 JdbcTemplate, 验证:
 *   - exportClassSchedule / exportTeacherSchedule 的 SQL 参数顺序 (semesterId, orgUnitId/teacherId)
 *   - 标题查询失败 (queryForMap 抛异常) 时降级到默认标题, 仍能产出 Excel
 *   - 生成的 .xlsx 字节可被 POI 重新解析, 含标题行 / 表头行 / 10 节次行
 *   - 课程条目按 weekday / start_slot..end_slot 正确落入网格单元格
 *   - 越界 weekday / slot 被安全忽略
 *
 * 真正的 POI 渲染通过解析回写出的 workbook 做端到端断言, 而非 mock OutputStream.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleExportService 课表导出服务")
class ScheduleExportServiceTest {

    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks ScheduleExportService service;

    // ---------- helpers ----------

    private Map<String, Object> entry(int weekday, int startSlot, int endSlot,
                                      String course, String teacher,
                                      String className, String classroom) {
        Map<String, Object> m = new HashMap<>();
        m.put("weekday", weekday);
        m.put("start_slot", startSlot);
        m.put("end_slot", endSlot);
        m.put("course_name", course);
        m.put("teacher_name", teacher);
        m.put("class_name", className);
        m.put("classroom_name", classroom == null ? "" : classroom);
        return m;
    }

    private Workbook parse(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    /** Returns the cell value at the given period (0-based) and day (0-based). */
    private String cellAt(Sheet sheet, int period, int day) {
        Row row = sheet.getRow(period + 2);
        Cell cell = row.getCell(day + 1);
        return cell.getStringCellValue();
    }

    // ============================================================
    @Nested
    @DisplayName("exportClassSchedule")
    class ExportClassScheduleTests {

        @Test
        @DisplayName("查询参数顺序: semesterId 在前, orgUnitId 在后")
        void shouldPassSemesterThenOrgUnit() throws IOException {
            when(jdbcTemplate.queryForList(contains("schedule_entries"), eq(11L), eq(22L)))
                    .thenReturn(List.of());
            when(jdbcTemplate.queryForMap(contains("school_classes"), eq(22L)))
                    .thenReturn(Map.of("name", "高一(3)班"));

            byte[] bytes = service.exportClassSchedule(11L, 22L);

            assertThat(bytes).isNotEmpty();
            verify(jdbcTemplate).queryForList(contains("schedule_entries"), eq(11L), eq(22L));
            verify(jdbcTemplate).queryForMap(contains("school_classes"), eq(22L));
        }

        @Test
        @DisplayName("成功导出: 标题用班级名 + 课表后缀, 含 12 行 (标题+表头+10节次)")
        void shouldBuildWorkbookWithClassTitle() throws IOException {
            when(jdbcTemplate.queryForList(anyString(), eq(11L), eq(22L)))
                    .thenReturn(List.of());
            when(jdbcTemplate.queryForMap(anyString(), eq(22L)))
                    .thenReturn(Map.of("name", "高一(3)班"));

            byte[] bytes = service.exportClassSchedule(11L, 22L);

            try (Workbook wb = parse(bytes)) {
                Sheet sheet = wb.getSheet("课表");
                assertThat(sheet).isNotNull();
                assertThat(sheet.getRow(0).getCell(0).getStringCellValue())
                        .isEqualTo("高一(3)班 课表");
                // header row
                assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("节次");
                assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("周一");
                assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo("周五");
                // 10 period rows, rows 2..11
                assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("第1节");
                assertThat(sheet.getRow(11).getCell(0).getStringCellValue()).isEqualTo("第10节");
                assertThat(sheet.getRow(12)).isNull();
            }
        }

        @Test
        @DisplayName("班级名查询抛异常时降级为默认标题 '班级课表'")
        void shouldFallbackToDefaultTitleWhenClassQueryFails() throws IOException {
            when(jdbcTemplate.queryForList(anyString(), eq(11L), eq(22L)))
                    .thenReturn(List.of());
            when(jdbcTemplate.queryForMap(anyString(), eq(22L)))
                    .thenThrow(new EmptyResultDataAccessException(1));

            byte[] bytes = service.exportClassSchedule(11L, 22L);

            try (Workbook wb = parse(bytes)) {
                assertThat(wb.getSheet("课表").getRow(0).getCell(0).getStringCellValue())
                        .isEqualTo("班级课表");
            }
        }

        @Test
        @DisplayName("课程条目落入正确网格: weekday=1,slot 1-2 -> 周一第1/2节单元格")
        void shouldPlaceEntryIntoCorrectGridCells() throws IOException {
            when(jdbcTemplate.queryForList(anyString(), eq(11L), eq(22L)))
                    .thenReturn(List.of(entry(1, 1, 2, "数学", "张老师", "", "A101")));
            when(jdbcTemplate.queryForMap(anyString(), eq(22L)))
                    .thenReturn(Map.of("name", "高一"));

            byte[] bytes = service.exportClassSchedule(11L, 22L);

            try (Workbook wb = parse(bytes)) {
                Sheet sheet = wb.getSheet("课表");
                // period 0 (第1节) day 0 (周一)
                String c1 = cellAt(sheet, 0, 0);
                assertThat(c1).contains("数学").contains("张老师").contains("A101");
                // period 1 (第2节) day 0 — endSlot=2 covers slot 2 too
                assertThat(cellAt(sheet, 1, 0)).contains("数学");
                // unrelated cell stays empty
                assertThat(cellAt(sheet, 2, 0)).isEmpty();
            }
        }

        @Test
        @DisplayName("越界 weekday (6=周六) 与 slot>10 被安全忽略, 不抛异常")
        void shouldIgnoreOutOfBoundsEntries() throws IOException {
            when(jdbcTemplate.queryForList(anyString(), eq(11L), eq(22L)))
                    .thenReturn(List.of(
                            entry(6, 1, 1, "周末课", null, "", ""),     // weekday out of 1..5
                            entry(2, 11, 12, "超界课", null, "", "")));  // slot > 10
            when(jdbcTemplate.queryForMap(anyString(), eq(22L)))
                    .thenReturn(Map.of("name", "高一"));

            byte[] bytes = service.exportClassSchedule(11L, 22L);

            try (Workbook wb = parse(bytes)) {
                Sheet sheet = wb.getSheet("课表");
                // grid all empty — nothing placed
                for (int p = 0; p < 10; p++) {
                    for (int d = 0; d < 5; d++) {
                        assertThat(cellAt(sheet, p, d)).isEmpty();
                    }
                }
            }
        }
    }

    // ============================================================
    @Nested
    @DisplayName("exportTeacherSchedule")
    class ExportTeacherScheduleTests {

        @Test
        @DisplayName("查询参数顺序: semesterId 在前, teacherId 在后")
        void shouldPassSemesterThenTeacher() throws IOException {
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            when(jdbcTemplate.queryForList(contains("teacher_id"), eq(33L), eq(44L)))
                    .thenReturn(List.of());
            when(jdbcTemplate.queryForMap(contains("users"), eq(44L)))
                    .thenReturn(Map.of("real_name", "李老师"));

            byte[] bytes = service.exportTeacherSchedule(33L, 44L);

            assertThat(bytes).isNotEmpty();
            verify(jdbcTemplate).queryForList(contains("teacher_id"), eq(33L), eq(44L));
            verify(jdbcTemplate).queryForMap(contains("users"), eq(44L));
            // ArgumentCaptor over varargs Object[] — verifies queryForList received exactly 2 params
            verify(jdbcTemplate).queryForList(anyString(), captor.capture());
            assertThat(captor.getValue()).containsExactly(33L, 44L);
        }

        @Test
        @DisplayName("成功导出: 标题用教师名 + 课表后缀")
        void shouldBuildWorkbookWithTeacherTitle() throws IOException {
            when(jdbcTemplate.queryForList(anyString(), eq(33L), eq(44L)))
                    .thenReturn(List.of());
            when(jdbcTemplate.queryForMap(anyString(), eq(44L)))
                    .thenReturn(Map.of("real_name", "李老师"));

            byte[] bytes = service.exportTeacherSchedule(33L, 44L);

            try (Workbook wb = parse(bytes)) {
                assertThat(wb.getSheet("课表").getRow(0).getCell(0).getStringCellValue())
                        .isEqualTo("李老师 课表");
            }
        }

        @Test
        @DisplayName("教师名查询抛异常时降级为默认标题 '教师课表'")
        void shouldFallbackToDefaultTitleWhenTeacherQueryFails() throws IOException {
            when(jdbcTemplate.queryForList(anyString(), eq(33L), eq(44L)))
                    .thenReturn(List.of());
            when(jdbcTemplate.queryForMap(anyString(), eq(44L)))
                    .thenThrow(new RuntimeException("db down"));

            byte[] bytes = service.exportTeacherSchedule(33L, 44L);

            try (Workbook wb = parse(bytes)) {
                assertThat(wb.getSheet("课表").getRow(0).getCell(0).getStringCellValue())
                        .isEqualTo("教师课表");
            }
        }

        @Test
        @DisplayName("教师课表条目含 class_name: 单元格拼入班级名")
        void shouldIncludeClassNameInCell() throws IOException {
            when(jdbcTemplate.queryForList(anyString(), eq(33L), eq(44L)))
                    .thenReturn(List.of(entry(3, 5, 5, "物理", null, "高二(1)班", "B202")));
            when(jdbcTemplate.queryForMap(anyString(), eq(44L)))
                    .thenReturn(Map.of("real_name", "李老师"));

            byte[] bytes = service.exportTeacherSchedule(33L, 44L);

            try (Workbook wb = parse(bytes)) {
                // weekday 3 -> day index 2, slot 5 -> period index 4
                String c = cellAt(wb.getSheet("课表"), 4, 2);
                assertThat(c).contains("物理").contains("高二(1)班").contains("B202");
            }
        }
    }
}
