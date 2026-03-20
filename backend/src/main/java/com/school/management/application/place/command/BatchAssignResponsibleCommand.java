package com.school.management.application.place.command;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量分配负责人命令
 * 对标: AWS IAM - Batch Assign Roles
 */
@Data
public class BatchAssignResponsibleCommand {

    /**
     * 场所ID列表
     */
    @NotEmpty(message = "场所ID列表不能为空")
    private List<Long> placeIds;

    /**
     * 目标负责人ID
     * NULL 表示清除负责人
     */
    private Long targetResponsibleUserId;

    /**
     * 操作原因（用于审计）
     */
    private String reason;

    /**
     * 是否清除负责人
     */
    public boolean isClearingResponsible() {
        return targetResponsibleUserId == null;
    }
}
