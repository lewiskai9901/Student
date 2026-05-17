package com.school.management.interfaces.rest.inspection;

import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.inspection.InspectionScopeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 检查平台数据导出 — 给 HR/考核办/教务等数据消费者用.
 *
 * <p>四种 Excel 报表:
 * <ul>
 *   <li>{@code GET /inspection/export/ranking}     — 周期排名 (默认 30 天)</li>
 *   <li>{@code GET /inspection/export/corrective}  — 整改履约</li>
 *   <li>{@code GET /inspection/export/appeals}     — 申诉处理记录</li>
 *   <li>{@code GET /inspection/export/audit}       — 审计日志</li>
 * </ul>
 *
 * <p>所有端点返回 .xlsx 流, 已设 Content-Disposition 强制下载.
 */
@Slf4j
@RestController
@RequestMapping("/inspection/export")
@RequiredArgsConstructor
public class InspectionExportController {

    private final JdbcTemplate jdbcTemplate;
    private final InspectionScopeHelper scopeHelper;

    @GetMapping("/ranking")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public ResponseEntity<byte[]> exportRanking(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        StringBuilder sql = new StringBuilder("""
            SELECT summary_date, target_name, org_unit_name,
                   inspection_count, avg_score, min_score, max_score,
                   total_deductions, ranking, grade
            FROM insp_daily_summaries
            WHERE summary_date BETWEEN ? AND ? AND deleted = 0
            """);
        Object[] params = projectId != null
                ? new Object[]{start, end, projectId}
                : new Object[]{start, end};
        if (projectId != null) sql.append(" AND project_id = ?");
        sql.append(scopeHelper.orgScopeClause("org_unit_id"));   // I1: 旁路加数据权限
        sql.append(" ORDER BY summary_date DESC, ranking ASC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        return buildExcel("排名报表", new String[]{
                "日期", "受检目标", "组织单元", "检查次数", "平均分", "最低分", "最高分",
                "累计扣分", "排名", "等级"
        }, rows, new String[]{
                "summary_date", "target_name", "org_unit_name", "inspection_count",
                "avg_score", "min_score", "max_score", "total_deductions", "ranking", "grade"
        }, "排名_" + start + "_" + end + ".xlsx");
    }

    @GetMapping("/corrective")
    @CasbinAccess(resource = "insp:corrective", action = "view")
    public ResponseEntity<byte[]> exportCorrective(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status) {
        StringBuilder sql = new StringBuilder("""
            SELECT case_code, target_name, issue_description, priority, status,
                   deadline, assignee_name, escalation_level, created_at, verified_at
            FROM insp_corrective_cases
            WHERE deleted = 0
            """);
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (projectId != null) { sql.append(" AND project_id = ?"); params.add(projectId); }
        if (status != null) { sql.append(" AND status = ?"); params.add(status); }
        sql.append(scopeHelper.orgScopeClause("org_unit_id"));   // I1
        sql.append(" ORDER BY created_at DESC LIMIT 5000");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        return buildExcel("整改履约", new String[]{
                "案号", "目标", "问题描述", "优先级", "状态", "截止时间",
                "整改人", "升级层级", "创建时间", "验证时间"
        }, rows, new String[]{
                "case_code", "target_name", "issue_description", "priority", "status",
                "deadline", "assignee_name", "escalation_level", "created_at", "verified_at"
        }, "整改履约_" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/appeals")
    @CasbinAccess(resource = "insp:appeal", action = "view")
    public ResponseEntity<byte[]> exportAppeals() {
        // I1: 旁路加数据权限
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT appeal_code, submitter_name, reason, status, expected_adjustment, " +
            "       final_adjustment, reviewer_name, reviewer_comment, " +
            "       created_at, reviewed_at " +
            "FROM inspection_appeals " +
            "WHERE deleted = 0" + scopeHelper.orgScopeClause("org_unit_id") +
            " ORDER BY created_at DESC LIMIT 5000");

        return buildExcel("申诉记录", new String[]{
                "申诉编号", "申诉人", "申诉理由", "状态", "期望调整", "最终调整",
                "审核员", "审核意见", "提交时间", "审核时间"
        }, rows, new String[]{
                "appeal_code", "submitter_name", "reason", "status", "expected_adjustment",
                "final_adjustment", "reviewer_name", "reviewer_comment", "created_at", "reviewed_at"
        }, "申诉记录_" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/audit")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public ResponseEntity<byte[]> exportAudit(
            @RequestParam(required = false) String aggregateType) {
        StringBuilder sql = new StringBuilder("""
            SELECT occurred_at, entity_type, entity_code, action,
                   actor_user_name, reason
            FROM inspection_audit_logs
            WHERE 1=1
            """);
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (aggregateType != null) { sql.append(" AND entity_type = ?"); params.add(aggregateType); }
        // I4: 加 org_unit_id scope (允许 NULL 通过, 兼容历史无 org 数据)
        var ids = scopeHelper.allowedOrgIds();
        if (ids != null) {
            if (ids.isEmpty()) {
                sql.append(" AND 1=0");
            } else {
                String csv = ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
                sql.append(" AND (org_unit_id IS NULL OR org_unit_id IN (").append(csv).append("))");
            }
        }
        sql.append(" ORDER BY occurred_at DESC LIMIT 5000");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        return buildExcel("审计日志", new String[]{
                "时间", "聚合类型", "聚合编号", "操作", "操作人", "备注"
        }, rows, new String[]{
                "occurred_at", "entity_type", "entity_code", "action",
                "actor_user_name", "reason"
        }, "审计日志_" + LocalDate.now() + ".xlsx");
    }

    /** 通用 Excel 构建 — POI XSSFWorkbook 流式输出 */
    private ResponseEntity<byte[]> buildExcel(String sheetName, String[] headers,
                                              List<Map<String, Object>> rows, String[] columns,
                                              String fileName) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            // 表头样式
            CellStyle headStyle = wb.createCellStyle();
            Font headFont = wb.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }

            // 数据
            int r = 1;
            for (Map<String, Object> row : rows) {
                Row excelRow = sheet.createRow(r++);
                for (int i = 0; i < columns.length; i++) {
                    Object v = row.get(columns[i]);
                    Cell cell = excelRow.createCell(i);
                    if (v == null) cell.setCellValue("");
                    else if (v instanceof Number n) cell.setCellValue(n.doubleValue());
                    else cell.setCellValue(v.toString());
                }
            }

            // 自适应列宽 (限制最大 256*20 字符防超大列)
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int w = sheet.getColumnWidth(i);
                if (w > 256 * 30) sheet.setColumnWidth(i, 256 * 30);
            }

            wb.write(out);
            byte[] bytes = out.toByteArray();

            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            HttpHeaders headers2 = new HttpHeaders();
            headers2.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers2.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + encoded);

            return new ResponseEntity<>(bytes, headers2, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            log.error("导出 Excel 失败 sheet={}", sheetName, e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }
}
