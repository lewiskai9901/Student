package com.school.management.application.inspection.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 人员工作台聚合视图 — Phase B Frontend「按人」视图主数据源.
 *
 * <p>一次拉取 inspectors + per-person stats + 4 段任务分组 + 项目级 summary,
 * 避免前端 N+1 (旧方案要 listInspectors + 多次 task 查询).
 */
@Data
@Builder
public class PeopleWorkbenchView {

    private Long projectId;
    private String projectCode;
    private String projectName;

    /** 顶部状态条 - 项目级 summary */
    private Summary summary;

    /** 主区域 - 每个人一行 */
    private List<PersonRow> people;

    /** 共享池 - 待分配任务 (与具体 inspector 无关, 列在「按任务」视图) */
    private List<TaskRow> pendingAssignTasks;

    @Data
    @Builder
    public static class Summary {
        private int totalPeople;
        private int totalInspectors;
        private int totalReviewers;
        private int totalLeads;
        private int pendingAssignCount;
        private int pendingReviewCount;
        private int inProgressCount;
        private int overdueCount;
        /** 第一个 LEAD 的展示名 (顶部状态条用) */
        private String leadName;
    }

    @Data
    @Builder
    public static class PersonRow {
        private Long userId;
        private String userName;
        private String orgUnitName;     // 可选, 部门归属
        private List<String> roles;     // INSPECTOR / REVIEWER / LEAD (允许多角色)
        private boolean isActive;
        private boolean isCreator;      // 是否本项目创建者 (createdBy)
        private PersonStats stats;
        private PersonTasks tasks;      // 4 段分组 — 默认折叠时仅看 stats, 展开后看 tasks
    }

    @Data
    @Builder
    public static class PersonStats {
        /** 本周分配数 (近 7 天) */
        private int weekAssigned;
        /** 本周完成数 */
        private int weekCompleted;
        /** 待分配候选数 — 系统判定该人为候选 (此 v1 简化: 全项目 PENDING 任务都算候选, 前端可显示" 忙/空闲") */
        private int pendingAssignCandidates;
        /** 进行中: CLAIMED / IN_PROGRESS / SUBMITTED 作为 inspectorId */
        private int inProgress;
        /** 待我审核: UNDER_REVIEW 作为 reviewerId, 或 SUBMITTED 但项目要求审核 (作为 reviewer 候选) */
        private int pendingReview;
        /** 逾期: 进行中 + taskDate < today */
        private int overdue;
        /** 完整周期 (项目期间) 分配数 - 用于负载饱和度参考 */
        private int totalAssigned;
        /** 完整周期完成数 */
        private int totalCompleted;
    }

    @Data
    @Builder
    public static class PersonTasks {
        private List<TaskRow> inProgress;
        private List<TaskRow> pendingReview;
        private List<TaskRow> overdue;
    }

    @Data
    @Builder
    public static class TaskRow {
        private Long taskId;
        private String taskCode;
        private LocalDate taskDate;
        private String status;          // PENDING / CLAIMED / ... TaskStatus enum name
        private Long inspectorId;
        private String inspectorName;
        private Long reviewerId;
        private String reviewerName;
        private Integer totalTargets;
        private Integer completedTargets;
        private LocalDateTime submittedAt;
        private Integer daysOverdue;    // 0 表示未逾期; >0 表示逾期天数
    }
}
