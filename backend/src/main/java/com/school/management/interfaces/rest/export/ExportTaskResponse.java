package com.school.management.interfaces.rest.export;

import com.school.management.application.export.query.ExportTaskDTO;
import java.time.LocalDateTime;

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

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
