package com.school.management.application.export;

import com.school.management.application.export.command.CreateExportCommand;
import com.school.management.application.export.query.ExportTaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionExportService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public ExportTaskDTO createExportTask(CreateExportCommand command) {
        String taskCode = "EXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        jdbcTemplate.update(
            "INSERT INTO export_tasks (task_code, export_type, export_format, status, progress, request_data, created_by, created_at) VALUES (?, ?, ?, 'PENDING', 0, ?, ?, ?)",
            taskCode, command.getExportType(), command.getExportFormat(),
            command.getFilters(), command.getCreatedBy(), LocalDateTime.now()
        );

        // Start async processing
        processExportAsync(taskCode);

        ExportTaskDTO dto = new ExportTaskDTO();
        dto.setTaskCode(taskCode);
        dto.setExportType(command.getExportType());
        dto.setExportFormat(command.getExportFormat());
        dto.setStatus("PENDING");
        dto.setProgress(0);
        dto.setCreatedBy(command.getCreatedBy());
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    @Async
    protected void processExportAsync(String taskCode) {
        try {
            // Update status to PROCESSING
            jdbcTemplate.update("UPDATE export_tasks SET status = 'PROCESSING' WHERE task_code = ?", taskCode);

            // Simulate export processing - actual implementation would generate files
            log.info("Processing export task: {}", taskCode);

            // Update to COMPLETED
            jdbcTemplate.update(
                "UPDATE export_tasks SET status = 'COMPLETED', progress = 100, completed_at = ? WHERE task_code = ?",
                LocalDateTime.now(), taskCode
            );
            log.info("Export task completed: {}", taskCode);
        } catch (Exception e) {
            log.error("Export task failed: {}", taskCode, e);
            jdbcTemplate.update(
                "UPDATE export_tasks SET status = 'FAILED', error_message = ? WHERE task_code = ?",
                e.getMessage(), taskCode
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ExportTaskDTO> listMyTasks(Long userId) {
        return jdbcTemplate.query(
            "SELECT * FROM export_tasks WHERE created_by = ? ORDER BY created_at DESC LIMIT 50",
            (rs, rowNum) -> {
                ExportTaskDTO dto = new ExportTaskDTO();
                dto.setId(rs.getLong("id"));
                dto.setTaskCode(rs.getString("task_code"));
                dto.setExportType(rs.getString("export_type"));
                dto.setExportFormat(rs.getString("export_format"));
                dto.setStatus(rs.getString("status"));
                dto.setProgress(rs.getInt("progress"));
                dto.setFileName(rs.getString("file_name"));
                dto.setFileSize(rs.getObject("file_size") != null ? rs.getLong("file_size") : null);
                dto.setDownloadUrl(rs.getString("download_url"));
                dto.setErrorMessage(rs.getString("error_message"));
                dto.setCreatedBy(rs.getLong("created_by"));
                dto.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
                dto.setCompletedAt(rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toLocalDateTime() : null);
                return dto;
            },
            userId
        );
    }

    @Transactional(readOnly = true)
    public ExportTaskDTO getTask(String taskCode) {
        List<ExportTaskDTO> tasks = jdbcTemplate.query(
            "SELECT * FROM export_tasks WHERE task_code = ?",
            (rs, rowNum) -> {
                ExportTaskDTO dto = new ExportTaskDTO();
                dto.setId(rs.getLong("id"));
                dto.setTaskCode(rs.getString("task_code"));
                dto.setExportType(rs.getString("export_type"));
                dto.setExportFormat(rs.getString("export_format"));
                dto.setStatus(rs.getString("status"));
                dto.setProgress(rs.getInt("progress"));
                dto.setFileName(rs.getString("file_name"));
                dto.setFileSize(rs.getObject("file_size") != null ? rs.getLong("file_size") : null);
                dto.setDownloadUrl(rs.getString("download_url"));
                dto.setErrorMessage(rs.getString("error_message"));
                dto.setCreatedBy(rs.getLong("created_by"));
                dto.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
                dto.setCompletedAt(rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toLocalDateTime() : null);
                return dto;
            },
            taskCode
        );
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("Export task not found: " + taskCode);
        }
        return tasks.get(0);
    }
}
