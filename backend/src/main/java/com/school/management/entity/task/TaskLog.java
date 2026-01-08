package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务操作日志实体(审计追踪)
 */
@Data
@TableName(value = "task_logs", autoResultMap = true)
public class TaskLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作类型: CREATE/ASSIGN/ACCEPT/SUBMIT/APPROVE/REJECT/CANCEL/TRANSFER
     */
    private String action;

    /**
     * 操作描述
     */
    private String actionDesc;

    /**
     * 操作前状态
     */
    private Integer oldStatus;

    /**
     * 操作后状态
     */
    private Integer newStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 额外数据
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraData;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
