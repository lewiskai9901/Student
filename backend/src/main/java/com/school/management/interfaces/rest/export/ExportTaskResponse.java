package com.school.management.interfaces.rest.export;

import com.school.management.application.export.query.ExportTaskDTO;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExportTaskResponse {
    private Long id;
    private String taskCode;
    private String exportType;
    private String exportFormat;
    private String status;
    private Integer progress;
    private String fileName;
    private Long fileSize;
    private String downloadUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static ExportTaskResponse fromDTO(ExportTaskDTO dto) {
        ExportTaskResponse response = new ExportTaskResponse();
        response.setId(dto.getId());
        response.setTaskCode(dto.getTaskCode());
        response.setExportType(dto.getExportType());
        response.setExportFormat(dto.getExportFormat());
        response.setStatus(dto.getStatus());
        response.setProgress(dto.getProgress());
        response.setFileName(dto.getFileName());
        response.setFileSize(dto.getFileSize());
        response.setDownloadUrl(dto.getDownloadUrl());
        response.setErrorMessage(dto.getErrorMessage());
        response.setCreatedAt(dto.getCreatedAt());
        response.setCompletedAt(dto.getCompletedAt());
        return response;
    }
}
