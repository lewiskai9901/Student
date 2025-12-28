package com.school.management.dto.task;

import com.school.management.entity.task.Task;
import com.school.management.entity.task.TaskAssignee;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 任务详情DTO（含执行人卡片数据）
 */
@Data
public class TaskDetailDTO {

    /**
     * 任务基本信息
     */
    private Task task;

    /**
     * 审批流程配置（按系部）
     */
    private List<DepartmentApprovalFlowDTO> approvalFlows;

    /**
     * 执行人列表（按系部分组）
     */
    private List<DepartmentAssigneesDTO> assigneesByDepartment;

    /**
     * 统计信息
     */
    private TaskStatisticsDTO statistics;

    /**
     * 部门审批流程DTO
     */
    @Data
    public static class DepartmentApprovalFlowDTO {
        private Long departmentId;
        private String departmentName;
        private String flowChain; // 如: "班主任提交 → 张主任(系领导) → 王处长(学工处)"
    }

    /**
     * 部门执行人列表DTO
     */
    @Data
    public static class DepartmentAssigneesDTO {
        private Long departmentId;
        private String departmentName;
        private Integer totalCount;
        private Integer completedCount;
        private List<TaskAssigneeDetailDTO> assignees;
    }

    /**
     * 执行人详情DTO（卡片数据）
     */
    @Data
    public static class TaskAssigneeDetailDTO {
        private Long assigneeId;
        private String assigneeName;
        private Integer status;
        private String statusText;
        private Integer currentApprovalLevel;
        private List<ApprovalConfigDTO> approvalConfig;
        private List<ApprovalRecordDTO> approvalRecords;
        private String submittedAt;
        private String completedAt;
    }

    /**
     * 审批记录DTO
     */
    @Data
    public static class ApprovalRecordDTO {
        private Integer approvalLevel;
        private Integer approvalStatus;
        private String approvalTime;
    }
}
