package com.school.management.domain.teaching.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 监考安排实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamInvigilator {

    private Long id;

    /**
     * 考场ID
     */
    private Long roomId;

    /**
     * 监考教师ID
     */
    private Long teacherId;

    /**
     * 监考角色：1主监考 2副监考 3巡考
     */
    @Builder.Default
    private Integer invigilatorRole = 1;

    /**
     * 状态：1正常 0取消
     */
    @Builder.Default
    private Integer status = 1;

    /**
     * 备注
     */
    private String remark;

    private LocalDateTime createdAt;

    /**
     * 获取监考角色名称
     */
    public String getInvigilatorRoleName() {
        return switch (invigilatorRole) {
            case 1 -> "主监考";
            case 2 -> "副监考";
            case 3 -> "巡考";
            default -> "未知";
        };
    }

    /**
     * 是否为主监考
     */
    public boolean isMainInvigilator() {
        return invigilatorRole == 1;
    }

    /**
     * 取消监考安排
     */
    public void cancel() {
        this.status = 0;
    }

    /**
     * 恢复监考安排
     */
    public void restore() {
        this.status = 1;
    }

    /**
     * 是否已取消
     */
    public boolean isCancelled() {
        return status == 0;
    }
}
