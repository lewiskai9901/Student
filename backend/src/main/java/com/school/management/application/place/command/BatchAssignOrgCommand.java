package com.school.management.application.place.command;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量分配组织单元命令
 * 对标: AWS S3 Batch Operations - Tagging
 */
@Data
public class BatchAssignOrgCommand {

    /**
     * 场所ID列表
     */
    @NotEmpty(message = "场所ID列表不能为空")
    private List<Long> placeIds;

    /**
     * 目标组织单元ID
     * NULL 表示清除覆盖,恢复继承
     */
    private Long targetOrgUnitId;

    /**
     * 操作原因（用于审计）
     */
    private String reason;

    /**
     * 是否清除组织覆盖
     * true: targetOrgUnitId 必须为 null
     * false: targetOrgUnitId 不能为 null
     */
    public boolean isClearingOverride() {
        return targetOrgUnitId == null;
    }
}
