package com.school.management.domain.teaching.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.teaching.model.entity.ScheduleEntry;
import com.school.management.domain.teaching.model.valueobject.ScheduleConflict;
import com.school.management.domain.teaching.model.valueobject.TimeSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课表聚合根
 * 管理某学期的完整课表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CourseSchedule extends AggregateRoot<Long> {

    private Long id;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 课表名称
     */
    private String scheduleName;

    /**
     * 状态：0草稿 1已发布
     */
    @Builder.Default
    private Integer status = 0;

    /**
     * 版本号 - 使用Long以匹配AggregateRoot基类
     */
    @Builder.Default
    private Long version = 1L;

    @Override
    public Long getVersion() {
        return version;
    }

    /**
     * 课表条目列表
     */
    @Builder.Default
    private List<ScheduleEntry> entries = new ArrayList<>();

    /**
     * 冲突列表
     */
    @Builder.Default
    private List<ScheduleConflict> conflicts = new ArrayList<>();

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 是否已发布
     */
    public boolean isPublished() {
        return status != null && status == 1;
    }

    /**
     * 添加排课条目
     */
    public void addEntry(ScheduleEntry entry) {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        entry.setSemesterId(this.semesterId);
        entry.setScheduleId(this.id);

        // 检测冲突
        List<ScheduleConflict> newConflicts = detectConflicts(entry);
        if (!newConflicts.isEmpty()) {
            conflicts.addAll(newConflicts);
            entry.setConflictFlag(1);
        }

        entries.add(entry);
    }

    /**
     * 移除排课条目
     */
    public void removeEntry(Long entryId) {
        if (entries != null) {
            entries.removeIf(e -> e.getId().equals(entryId));
            // 移除相关冲突
            if (conflicts != null) {
                conflicts.removeIf(c -> entryId.equals(c.getEntryId())
                        || entryId.equals(c.getConflictEntryId()));
            }
        }
    }

    /**
     * 检测冲突
     */
    public List<ScheduleConflict> detectConflicts(ScheduleEntry newEntry) {
        List<ScheduleConflict> detected = new ArrayList<>();

        if (entries == null) {
            return detected;
        }

        for (ScheduleEntry existing : entries) {
            if (existing.getId() != null && existing.getId().equals(newEntry.getId())) {
                continue; // 跳过自身
            }

            // 检查时间是否重叠
            if (!isTimeOverlap(newEntry, existing)) {
                continue;
            }

            // 检查周次是否重叠
            if (!isWeekOverlap(newEntry, existing)) {
                continue;
            }

            // 教师冲突
            if (newEntry.getTeacherId() != null
                    && newEntry.getTeacherId().equals(existing.getTeacherId())) {
                detected.add(ScheduleConflict.teacherConflict(
                        newEntry.getId(), existing.getId(), newEntry.getTeacherId()));
            }

            // 教室冲突
            if (newEntry.getClassroomId() != null
                    && newEntry.getClassroomId().equals(existing.getClassroomId())) {
                detected.add(ScheduleConflict.classroomConflict(
                        newEntry.getId(), existing.getId(), newEntry.getClassroomId()));
            }

            // 班级冲突
            if (newEntry.getClassId() != null
                    && newEntry.getClassId().equals(existing.getClassId())) {
                detected.add(ScheduleConflict.classConflict(
                        newEntry.getId(), existing.getId(), newEntry.getClassId()));
            }
        }

        return detected;
    }

    /**
     * 检查时间是否重叠
     */
    private boolean isTimeOverlap(ScheduleEntry a, ScheduleEntry b) {
        if (a.getWeekday() == null || b.getWeekday() == null) {
            return false;
        }
        if (!a.getWeekday().equals(b.getWeekday())) {
            return false;
        }
        // 检查节次重叠
        Integer aStart = a.getStartSlot() != null ? a.getStartSlot() : a.getSlot();
        Integer aEnd = a.getEndSlot() != null ? a.getEndSlot() : a.getSlot();
        Integer bStart = b.getStartSlot() != null ? b.getStartSlot() : b.getSlot();
        Integer bEnd = b.getEndSlot() != null ? b.getEndSlot() : b.getSlot();
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) {
            return false;
        }
        return !(aEnd < bStart || aStart > bEnd);
    }

    /**
     * 检查周次是否重叠
     */
    private boolean isWeekOverlap(ScheduleEntry a, ScheduleEntry b) {
        Integer aStartWeek = a.getStartWeek();
        Integer aEndWeek = a.getEndWeek();
        Integer bStartWeek = b.getStartWeek();
        Integer bEndWeek = b.getEndWeek();

        if (aStartWeek == null || aEndWeek == null || bStartWeek == null || bEndWeek == null) {
            return true; // 缺少信息时假设可能冲突
        }

        // 检查周次范围
        if (aEndWeek < bStartWeek || aStartWeek > bEndWeek) {
            return false;
        }
        // 检查单双周
        Integer aWeekType = a.getWeekType() != null ? a.getWeekType() : 0;
        Integer bWeekType = b.getWeekType() != null ? b.getWeekType() : 0;
        if (aWeekType != 0 && bWeekType != 0 && !aWeekType.equals(bWeekType)) {
            return false; // 一个单周一个双周不冲突
        }
        return true;
    }

    /**
     * 获取班级课表
     */
    public List<ScheduleEntry> getEntriesByClass(Long classId) {
        if (entries == null) return new ArrayList<>();
        return entries.stream()
                .filter(e -> classId.equals(e.getClassId()))
                .collect(Collectors.toList());
    }

    /**
     * 获取教师课表
     */
    public List<ScheduleEntry> getEntriesByTeacher(Long teacherId) {
        if (entries == null) return new ArrayList<>();
        return entries.stream()
                .filter(e -> teacherId.equals(e.getTeacherId()))
                .collect(Collectors.toList());
    }

    /**
     * 获取教室课表
     */
    public List<ScheduleEntry> getEntriesByClassroom(Long classroomId) {
        if (entries == null) return new ArrayList<>();
        return entries.stream()
                .filter(e -> classroomId.equals(e.getClassroomId()))
                .collect(Collectors.toList());
    }

    /**
     * 获取指定时间段的条目
     */
    public List<ScheduleEntry> getEntriesByTimeSlot(TimeSlot timeSlot) {
        if (entries == null) return new ArrayList<>();
        return entries.stream()
                .filter(e -> e.getWeekday() != null && e.getWeekday().equals(timeSlot.getWeekday()))
                .filter(e -> {
                    Integer startSlot = e.getStartSlot() != null ? e.getStartSlot() : e.getSlot();
                    Integer endSlot = e.getEndSlot() != null ? e.getEndSlot() : e.getSlot();
                    return startSlot != null && endSlot != null
                            && startSlot <= timeSlot.getSlot() && endSlot >= timeSlot.getSlot();
                })
                .collect(Collectors.toList());
    }

    /**
     * 检查时间段是否可用（教师）
     */
    public boolean isTimeSlotAvailableForTeacher(Long teacherId, TimeSlot timeSlot, int startWeek, int endWeek) {
        if (entries == null) return true;
        return entries.stream()
                .filter(e -> teacherId.equals(e.getTeacherId()))
                .filter(e -> e.getWeekday() != null && e.getWeekday().equals(timeSlot.getWeekday()))
                .filter(e -> {
                    Integer s = e.getStartSlot() != null ? e.getStartSlot() : e.getSlot();
                    Integer ed = e.getEndSlot() != null ? e.getEndSlot() : e.getSlot();
                    return s != null && ed != null && s <= timeSlot.getSlot() && ed >= timeSlot.getSlot();
                })
                .filter(e -> e.getStartWeek() != null && e.getEndWeek() != null
                        && !(e.getEndWeek() < startWeek || e.getStartWeek() > endWeek))
                .findAny()
                .isEmpty();
    }

    /**
     * 检查时间段是否可用（教室）
     */
    public boolean isTimeSlotAvailableForClassroom(Long classroomId, TimeSlot timeSlot, int startWeek, int endWeek) {
        if (entries == null) return true;
        return entries.stream()
                .filter(e -> classroomId.equals(e.getClassroomId()))
                .filter(e -> e.getWeekday() != null && e.getWeekday().equals(timeSlot.getWeekday()))
                .filter(e -> {
                    Integer s = e.getStartSlot() != null ? e.getStartSlot() : e.getSlot();
                    Integer ed = e.getEndSlot() != null ? e.getEndSlot() : e.getSlot();
                    return s != null && ed != null && s <= timeSlot.getSlot() && ed >= timeSlot.getSlot();
                })
                .filter(e -> e.getStartWeek() != null && e.getEndWeek() != null
                        && !(e.getEndWeek() < startWeek || e.getStartWeek() > endWeek))
                .findAny()
                .isEmpty();
    }

    /**
     * 发布课表
     */
    public void publish() {
        if (hasConflicts()) {
            throw new IllegalStateException("存在未解决的冲突，无法发布");
        }
        this.status = 1;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * 是否有冲突
     */
    public boolean hasConflicts() {
        return conflicts != null && !conflicts.isEmpty();
    }

    /**
     * 获取未解决的冲突数量
     */
    public long getUnresolvedConflictCount() {
        if (conflicts == null) return 0;
        return conflicts.stream()
                .filter(c -> !c.isResolved())
                .count();
    }
}
