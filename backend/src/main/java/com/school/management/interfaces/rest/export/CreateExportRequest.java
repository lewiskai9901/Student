package com.school.management.interfaces.rest.export;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateExportRequest {
    @NotBlank(message = "导出类型不能为空")
    private String exportType;
    @NotBlank(message = "导出格式不能为空")
    private String exportFormat;
    private String filters;
}
