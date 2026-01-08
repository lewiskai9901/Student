package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.TableName;
import com.school.management.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_templates")
public class TaskTemplate extends BaseEntity {

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板分类: MEETING-会议类, INSPECTION-检查类, REPORT-报告类, OTHER-其他
     */
    private String category;

    /**
     * 任务标题模板（可包含变量如{date}）
     */
    private String titleTemplate;

    /**
     * 任务描述模板
     */
    private String descriptionTemplate;

    /**
     * 默认优先级: 1-紧急, 2-普通, 3-低
     */
    private Integer defaultPriority;

    /**
     * 默认截止时长（小时）
     */
    private Integer defaultDueDurationHours;

    /**
     * 默认流程模板ID
     */
    private Long defaultWorkflowTemplateId;

    /**
     * 是否启用: 0-禁用, 1-启用
     */
    private Integer enabled;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建人姓名
     */
    private String creatorName;

    /**
     * 使用次数
     */
    private Integer usageCount;
}
