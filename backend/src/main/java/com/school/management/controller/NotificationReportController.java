package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.entity.NotificationRecord;
import com.school.management.service.NotificationReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 通报生成控制器
 *
 * @author system
 * @since 4.2.0
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "通报管理", description = "通报生成与管理接口")
public class NotificationReportController {

    private final NotificationReportService notificationReportService;

    @PostMapping("/check-plans/{planId}/notifications/generate")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    @Operation(summary = "生成通报草稿", description = "根据选择的检查和模板生成可编辑的通报草稿")
    public Result<NotificationRecord> generateNotification(
            @PathVariable Long planId,
            @RequestBody GenerateRequest request) {
        log.info("生成通报草稿请求: planId={}, templateId={}, deductionItemIds={}",
                planId, request.getTemplateId(), request.getDeductionItemIds());
        request.setPlanId(planId);
        NotificationRecord record = notificationReportService.generateNotification(
                planId,
                request.getTemplateId(),
                request.getDailyCheckIds(),
                request.getCheckRounds(),
                request.getDeductionItemIds(),
                request.getVariableValues()
        );
        return Result.success(record);
    }

    @GetMapping("/check-plans/{planId}/notifications")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    @Operation(summary = "获取通报历史", description = "获取指定计划的通报生成历史")
    public Result<List<NotificationRecord>> getHistory(@PathVariable Long planId) {
        List<NotificationRecord> history = notificationReportService.getNotificationHistory(planId);
        return Result.success(history);
    }

    @GetMapping("/notifications/{id}")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    @Operation(summary = "获取通报详情", description = "获取通报记录详情")
    public Result<NotificationRecord> getNotification(@PathVariable Long id) {
        NotificationRecord record = notificationReportService.getNotificationById(id);
        return Result.success(record);
    }

    @PostMapping("/notifications/{id}/regenerate")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    @Operation(summary = "重新生成通报", description = "使用相同参数重新生成通报")
    public Result<NotificationRecord> regenerate(@PathVariable Long id) {
        NotificationRecord record = notificationReportService.regenerateNotification(id);
        return Result.success(record);
    }

    @DeleteMapping("/notifications/{id}")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    @Operation(summary = "删除通报", description = "删除通报记录及其文件")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        notificationReportService.deleteNotification(id);
        return Result.success();
    }

    @PutMapping("/notifications/{id}/content")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    @Operation(summary = "更新通报内容", description = "更新通报的标题和HTML内容")
    public Result<NotificationRecord> updateContent(
            @PathVariable Long id,
            @RequestBody UpdateContentRequest request) {
        log.info("更新通报内容: id={}", id);
        NotificationRecord record = notificationReportService.updateNotificationContent(
                id, request.getTitle(), request.getContentHtml());
        return Result.success(record);
    }

    @PostMapping("/notifications/{id}/publish")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    @Operation(summary = "发布通报", description = "将草稿通报发布")
    public Result<NotificationRecord> publishNotification(@PathVariable Long id) {
        log.info("发布通报: id={}", id);
        NotificationRecord record = notificationReportService.publishNotification(id);
        return Result.success(record);
    }

    @GetMapping("/notifications/{id}/download")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    @Operation(summary = "下载通报文件", description = "按需生成并下载通报文件")
    public ResponseEntity<byte[]> downloadNotification(
            @PathVariable Long id,
            @RequestParam(defaultValue = "PDF") String format) {
        NotificationRecord record = notificationReportService.getNotificationById(id);
        byte[] fileBytes = notificationReportService.downloadNotification(id, format);

        String contentType = getContentType(format);
        String extension = getExtension(format);
        String fileName = (record.getTitle() != null ? record.getTitle() : "通报") + extension;
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(fileBytes.length)
                .body(fileBytes);
    }

    @GetMapping("/notifications/{id}/preview")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    @Operation(summary = "预览通报内容", description = "获取通报的HTML内容用于预览")
    public Result<PreviewResponse> previewNotification(@PathVariable Long id) {
        NotificationRecord record = notificationReportService.getNotificationById(id);
        PreviewResponse response = new PreviewResponse();
        response.setHtml(record.getContentSnapshot());
        response.setTitle(record.getTitle());
        return Result.success(response);
    }

    private String getContentType(String format) {
        if (format == null) return "application/pdf";
        return switch (format.toUpperCase()) {
            case "WORD" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/pdf";
        };
    }

    private String getExtension(String format) {
        if (format == null) return ".pdf";
        return switch (format.toUpperCase()) {
            case "WORD" -> ".docx";
            case "EXCEL" -> ".xlsx";
            default -> ".pdf";
        };
    }

    // ========== 请求/响应DTO ==========

    @Data
    public static class GenerateRequest {
        private Long planId;
        private Long templateId;
        private List<Long> dailyCheckIds;
        private List<Integer> checkRounds;
        private List<Long> deductionItemIds;  // 扣分项筛选，为空则包含全部
        private String variableValues;
    }

    @Data
    public static class UpdateContentRequest {
        private String title;
        private String contentHtml;
    }

    @Data
    public static class PreviewResponse {
        private String title;
        private String html;
    }
}
