package com.school.management.dto.quickentry;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 快捷录入 - 提交请求DTO
 *
 * @author system
 * @since 1.0.7
 */
@Data
public class QuickEntrySubmitRequest {

    /**
     * 扣分项ID
     */
    @NotNull(message = "扣分项ID不能为空")
    private Long deductionItemId;

    /**
     * 学生ID
     */
    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    /**
     * 检查轮次（默认为1）
     */
    private Integer checkRound;

    /**
     * 备注 (可选)
     */
    private String remark;

    /**
     * 照片URL列表 (可选)
     */
    private List<String> photoUrls;
}
