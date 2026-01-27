package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批通过/拒绝命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveCommand {

    @NotNull(message = "审批ID不能为空")
    private Long approvalId;

    @NotNull(message = "审批人ID不能为空")
    private Long approverId;

    private String approverName;

    private String remark;

    /** 是否通过 */
    private boolean approved;
}
