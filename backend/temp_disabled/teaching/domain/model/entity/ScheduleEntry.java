package com.school.management.domain.teaching.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课表条目实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEntry {

    private Long id;

    /**
     * 课表ID
     */
    private Long scheduleId;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 教学任务ID
     */
    private Long taskId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 主讲教师ID
     */
    private Long teacherId;

    /**
     * 教室ID
     */
    private Long classroomId;

    /**
     * 周几（1-7，1=周一）
     */
    private Integer weekday;

    /**
     * 节次（单节课时使用）
     */
    private Integer slot;

    /**
     * 开始节次
     */
    private Integer startSlot;

    /**
     * 结束节次（支持连堂）
     */
    private Integer endSlot;

    /**
     * 起始周次
     */
    @Builder.Default
    private Integer startWeek = 1;

    /**
     * 结束周次
     */
    @Builder.Default
    private Integer endWeek = 16;

    /**
     * 单双周：0每周 1单周 2双周
     */
    @Builder.Default
    private Integer weekType = 0;

    /**
     * 课程类型：1正常 2实验 3实践
     */
    @Builder.Default
    private Integer scheduleType = 1;

    /**
     * 状态：1正常 2暂停 0删除
     */
    @Builder.Default
    private Integer entryStatus = 1;

    /**
     * 冲突标记：0无冲突 1有冲突（强制保存）
     */
    @Builder.Default
    private Integer conflictFlag = 0;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    /**
     * 获取周几名称
     */
    public String getWeekdayName() {
        return switch (weekday) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "未知";
        };
    }

    /**
     * 获取单双周名称
     */
    public String getWeekTypeName() {
        return switch (weekType) {
            case 0 -> "每周";
            case 1 -> "单周";
            case 2 -> "双周";
            default -> "未知";
        };
    }

    /**
     * 获取节次描述
     */
    public String getSlotDescription() {
        if (startSlot.equals(endSlot)) {
            return "第" + startSlot + "节";
        }
        return "第" + startSlot + "-" + endSlot + "节";
    }

    /**
     * 获取周次描述
     */
    public String getWeekDescription() {
        String base = startWeek + "-" + endWeek + "周";
        if (weekType == 1) {
            return base + "(单)";
        } else if (weekType == 2) {
            return base + "(双)";
        }
        return base;
    }

    /**
     * 计算课时数
     */
    public int getHours() {
        return endSlot - startSlot + 1;
    }

    /**
     * 计算周数
     */
    public int getWeeks() {
        int totalWeeks = endWeek - startWeek + 1;
        if (weekType != 0) {
            return (totalWeeks + 1) / 2; // 单双周减半
        }
        return totalWeeks;
    }

    /**
     * 计算总课时
     */
    public int getTotalHours() {
        return getHours() * getWeeks();
    }

    /**
     * 检查是否在指定周上课
     */
    public boolean isActiveInWeek(int weekNumber) {
        if (weekNumber < startWeek || weekNumber > endWeek) {
            return false;
        }
        if (weekType == 0) {
            return true;
        }
        boolean isOddWeek = (weekNumber % 2 == 1);
        return (weekType == 1 && isOddWeek) || (weekType == 2 && !isOddWeek);
    }

    /**
     * 暂停课程
     */
    public void pause() {
        this.entryStatus = 2;
    }

    /**
     * 恢复课程
     */
    public void resume() {
        this.entryStatus = 1;
    }

    /**
     * 是否有冲突
     */
    public boolean hasConflict() {
        return conflictFlag == 1;
    }

    /**
     * 是否正常状态
     */
    public boolean isActive() {
        return entryStatus == 1;
    }
}
