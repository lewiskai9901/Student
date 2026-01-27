package com.school.management.domain.teaching.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教学任务教师分配实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTeacher {

    private Long id;

    /**
     * 教学任务ID
     */
    private Long taskId;

    /**
     * 教师ID
     */
    private Long teacherId;

    /**
     * 教师角色：1主讲 2辅讲 3助教
     */
    @Builder.Default
    private Integer teacherRole = 1;

    /**
     * 工作量比例
     */
    @Builder.Default
    private BigDecimal workloadRatio = BigDecimal.ONE;

    /**
     * 备注
     */
    private String remark;

    private LocalDateTime createdAt;

    /**
     * 获取教师角色名称
     */
    public String getTeacherRoleName() {
        return switch (teacherRole) {
            case 1 -> "主讲";
            case 2 -> "辅讲";
            case 3 -> "助教";
            default -> "未知";
        };
    }

    /**
     * 是否为主讲教师
     */
    public boolean isMainTeacher() {
        return teacherRole == 1;
    }

    /**
     * 设为主讲
     */
    public void setAsMain() {
        this.teacherRole = 1;
    }

    /**
     * 设为辅讲
     */
    public void setAsAssistant() {
        this.teacherRole = 2;
    }

    /**
     * 设为助教
     */
    public void setAsTA() {
        this.teacherRole = 3;
    }
}
