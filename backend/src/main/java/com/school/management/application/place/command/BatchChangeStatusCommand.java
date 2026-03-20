package com.school.management.application.place.command;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量变更状态命令
 * 对标: AWS EC2 - Batch Change Instance State
 */
@Data
public class BatchChangeStatusCommand {

    /**
     * 场所ID列表
     */
    @NotEmpty(message = "场所ID列表不能为空")
    private List<Long> placeIds;

    /**
     * 目标状态
     * - NORMAL: 正常
     * - DISABLED: 停用
     * - MAINTENANCE: 维护中
     */
    @NotNull(message = "目标状态不能为空")
    private String targetStatus;

    /**
     * 操作原因（用于审计）
     */
    private String reason;

    /**
     * 验证状态值是否合法
     */
    public void validateStatus() {
        if (targetStatus == null) {
            throw new IllegalArgumentException("目标状态不能为空");
        }
        if (!List.of("NORMAL", "DISABLED", "MAINTENANCE").contains(targetStatus)) {
            throw new IllegalArgumentException("非法的状态值: " + targetStatus);
        }
    }
}
