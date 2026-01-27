package com.school.management.domain.teaching.service;

import com.school.management.domain.teaching.model.entity.ScheduleEntry;
import com.school.management.domain.teaching.model.valueobject.ScheduleConflict;

import java.util.List;

/**
 * 冲突检测领域服务
 */
public interface ConflictDetectionService {

    /**
     * 检测排课冲突
     *
     * @param taskId      教学任务ID
     * @param weekday     星期几
     * @param slot        节次
     * @param startWeek   起始周
     * @param endWeek     结束周
     * @param weekType    单双周
     * @param classroomId 教室ID
     * @return 冲突列表
     */
    List<ScheduleConflict> detectConflicts(Long taskId, Integer weekday, Integer slot,
                                           Integer startWeek, Integer endWeek,
                                           Integer weekType, Long classroomId);

    /**
     * 检测单个条目的所有冲突
     *
     * @param entry           待检测的排课条目
     * @param existingEntries 已有的排课条目
     * @return 冲突列表
     */
    List<ScheduleConflict> detectAll(ScheduleEntry entry, List<ScheduleEntry> existingEntries);

    /**
     * 检测教师冲突
     *
     * @param entry           待检测的排课条目
     * @param existingEntries 已有的排课条目
     * @return 教师冲突列表
     */
    List<ScheduleConflict> detectTeacherConflicts(ScheduleEntry entry, List<ScheduleEntry> existingEntries);

    /**
     * 检测教室冲突
     *
     * @param entry           待检测的排课条目
     * @param existingEntries 已有的排课条目
     * @return 教室冲突列表
     */
    List<ScheduleConflict> detectClassroomConflicts(ScheduleEntry entry, List<ScheduleEntry> existingEntries);

    /**
     * 检测班级冲突
     *
     * @param entry           待检测的排课条目
     * @param existingEntries 已有的排课条目
     * @return 班级冲突列表
     */
    List<ScheduleConflict> detectClassConflicts(ScheduleEntry entry, List<ScheduleEntry> existingEntries);

    /**
     * 检测两个条目之间的冲突
     *
     * @param entry1 条目1
     * @param entry2 条目2
     * @return 冲突列表
     */
    List<ScheduleConflict> detectBetween(ScheduleEntry entry1, ScheduleEntry entry2);

    /**
     * 检测时间是否重叠
     *
     * @param entry1 条目1
     * @param entry2 条目2
     * @return 是否重叠
     */
    boolean isTimeOverlap(ScheduleEntry entry1, ScheduleEntry entry2);

    /**
     * 检测周次是否重叠
     *
     * @param entry1 条目1
     * @param entry2 条目2
     * @return 是否重叠
     */
    boolean isWeekOverlap(ScheduleEntry entry1, ScheduleEntry entry2);
}
