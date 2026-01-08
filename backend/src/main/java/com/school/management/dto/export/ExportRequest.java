package com.school.management.dto.export;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 导出请求
 */
@Data
public class ExportRequest {

    /**
     * 导出模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    /**
     * 选择的班级ID列表（为空则导出全部）
     */
    private List<Long> classIds;

    /**
     * 选择的检查轮次（为空则导出全部轮次）
     */
    private List<Integer> checkRounds;

    /**
     * 覆盖默认输出格式（可选）
     */
    private String format;
}
