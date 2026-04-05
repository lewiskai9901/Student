package com.school.management.domain.teaching.model.scheduling;

import com.school.management.domain.shared.AggregateRoot;
import java.time.LocalDateTime;

/**
 * 课表条目聚合根
 * 代表一条排课记录: 某门课在某个时间段由某教师在某教室上课
 */
public class ScheduleEntry extends AggregateRoot<Long> {
    private Long semesterId;
    private Long taskId;
    private Long teachingClassId;
    private Long courseId;
    private Long classId;
    private Long teacherId;
    private Long classroomId;
    private Integer weekday; // 1-7
    private Integer startSlot;
    private Integer endSlot;
    private Integer startWeek;
    private Integer endWeek;
    private WeekType weekType;
    private String consecutiveGroup;
    private Integer scheduleType;
    private Integer entryStatus; // 1=正常 0=取消
    private Integer conflictFlag;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ScheduleEntry() {}

    public static ScheduleEntry create(Long semesterId, Long taskId, Long courseId, Long classId,
                                        Long teacherId, Long classroomId, Integer weekday,
                                        Integer startSlot, Integer endSlot,
                                        Integer startWeek, Integer endWeek, WeekType weekType,
                                        Long createdBy) {
        if (semesterId == null) throw new IllegalArgumentException("学期不能为空");
        if (taskId == null) throw new IllegalArgumentException("教学任务不能为空");
        if (weekday == null || weekday < 1 || weekday > 7) throw new IllegalArgumentException("星期必须在1-7之间");
        if (startSlot == null || endSlot == null) throw new IllegalArgumentException("上课节次不能为空");
        if (endSlot < startSlot) throw new IllegalArgumentException("结束节次不能小于开始节次");

        ScheduleEntry entry = new ScheduleEntry();
        entry.semesterId = semesterId;
        entry.taskId = taskId;
        entry.courseId = courseId;
        entry.classId = classId;
        entry.teacherId = teacherId;
        entry.classroomId = classroomId;
        entry.weekday = weekday;
        entry.startSlot = startSlot;
        entry.endSlot = endSlot;
        entry.startWeek = startWeek != null ? startWeek : 1;
        entry.endWeek = endWeek != null ? endWeek : 16;
        entry.weekType = weekType != null ? weekType : WeekType.EVERY;
        entry.scheduleType = 1;
        entry.entryStatus = 1;
        entry.conflictFlag = 0;
        entry.createdBy = createdBy;
        entry.createdAt = LocalDateTime.now();
        entry.updatedAt = LocalDateTime.now();
        return entry;
    }

    public static ScheduleEntry reconstruct(Long id, Long semesterId, Long taskId, Long teachingClassId,
                                             Long courseId, Long classId, Long teacherId, Long classroomId,
                                             Integer weekday, Integer startSlot, Integer endSlot,
                                             Integer startWeek, Integer endWeek, WeekType weekType,
                                             String consecutiveGroup, Integer scheduleType,
                                             Integer entryStatus, Integer conflictFlag,
                                             Long createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        ScheduleEntry e = new ScheduleEntry();
        e.setId(id); e.semesterId = semesterId; e.taskId = taskId; e.teachingClassId = teachingClassId;
        e.courseId = courseId; e.classId = classId; e.teacherId = teacherId; e.classroomId = classroomId;
        e.weekday = weekday; e.startSlot = startSlot; e.endSlot = endSlot;
        e.startWeek = startWeek; e.endWeek = endWeek; e.weekType = weekType;
        e.consecutiveGroup = consecutiveGroup; e.scheduleType = scheduleType;
        e.entryStatus = entryStatus; e.conflictFlag = conflictFlag;
        e.createdBy = createdBy; e.createdAt = createdAt; e.updatedAt = updatedAt;
        return e;
    }

    /**
     * 移动课表条目到新的时间/教室
     */
    public void move(Integer newWeekday, Integer newStartSlot, Integer newEndSlot, Long newClassroomId) {
        if (this.entryStatus != null && this.entryStatus == 0) throw new IllegalStateException("已取消的课表不能移动");
        if (newWeekday != null) this.weekday = newWeekday;
        if (newStartSlot != null) this.startSlot = newStartSlot;
        if (newEndSlot != null) this.endSlot = newEndSlot;
        if (newClassroomId != null) this.classroomId = newClassroomId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否与另一条目在时间上冲突
     */
    public boolean conflictsWith(ScheduleEntry other) {
        if (other == null || this.getId().equals(other.getId())) return false;
        if (!this.semesterId.equals(other.semesterId)) return false;
        if (!this.weekday.equals(other.weekday)) return false;
        // 周次不重叠
        if (this.endWeek < other.startWeek || other.endWeek < this.startWeek) return false;
        // 单双周不冲突
        if (this.weekType != WeekType.EVERY && other.weekType != WeekType.EVERY
            && this.weekType != other.weekType) return false;
        // 节次重叠检查
        return this.startSlot <= other.endSlot && other.startSlot <= this.endSlot;
    }

    public void cancel() {
        this.entryStatus = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void markConflict() { this.conflictFlag = 1; }
    public void clearConflict() { this.conflictFlag = 0; }

    // Getters
    public Long getSemesterId() { return semesterId; }
    public Long getTaskId() { return taskId; }
    public Long getTeachingClassId() { return teachingClassId; }
    public Long getCourseId() { return courseId; }
    public Long getClassId() { return classId; }
    public Long getTeacherId() { return teacherId; }
    public Long getClassroomId() { return classroomId; }
    public Integer getWeekday() { return weekday; }
    public Integer getStartSlot() { return startSlot; }
    public Integer getEndSlot() { return endSlot; }
    public Integer getStartWeek() { return startWeek; }
    public Integer getEndWeek() { return endWeek; }
    public WeekType getWeekType() { return weekType; }
    public String getConsecutiveGroup() { return consecutiveGroup; }
    public Integer getScheduleType() { return scheduleType; }
    public Integer getEntryStatus() { return entryStatus; }
    public Integer getConflictFlag() { return conflictFlag; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
