package com.school.management.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务流程进度节点DTO
 */
@Data
public class TaskProgressNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点顺序: 1,2,3...
     */
    private Integer order;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型: CREATE-创建, EXECUTE-执行, APPROVE-审批, END-结束
     */
    private String nodeType;

    /**
     * 节点状态: COMPLETED-已完成, PROCESSING-进行中, PENDING-待处理, REJECTED-已打回
     */
    private String status;

    /**
     * 处理人姓名（单人节点）
     */
    private String handlerName;

    /**
     * 处理人ID（单人节点）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long handlerId;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handledAt;

    /**
     * 处理意见/备注
     */
    private String comment;

    /**
     * 处理人列表（多人节点，如批量任务的执行阶段）
     */
    private List<TaskAssigneeDTO> handlers;

    /**
     * 总人数（多人节点）
     */
    private Integer totalCount;

    /**
     * 已完成人数（多人节点）
     */
    private Integer completedCount;

    /**
     * 进度百分比（多人节点）
     */
    private Integer progressPercent;
}
