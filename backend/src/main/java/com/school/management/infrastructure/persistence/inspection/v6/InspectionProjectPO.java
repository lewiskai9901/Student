package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V6检查项目持久化对象
 */
@Data
@TableName("inspection_projects")
public class InspectionProjectPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String projectCode;
    private String projectName;
    private String description;

    // 模板相关
    private Long templateId;
    private String templateSnapshot;

    // 检查范围
    private String scopeType;
    private String scopeConfig;

    // 时间周期
    private LocalDate startDate;
    private LocalDate endDate;
    private String cycleType;
    private String cycleConfig;
    private String timeSlots;
    private Boolean skipHolidays;
    private String excludedDates;

    // 策略配置
    private String sharedPlaceStrategy;
    private String scoreDistributionMode;
    private String inspectorAssignmentMode;
    private String defaultInspectors;

    // 状态
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime completedAt;

    // 统计
    private Integer totalTasks;
    private Integer completedTasks;

    // 审计
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
