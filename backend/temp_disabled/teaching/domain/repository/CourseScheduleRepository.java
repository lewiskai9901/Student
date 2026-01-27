package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.aggregate.CourseSchedule;
import com.school.management.domain.teaching.model.entity.ScheduleEntry;
import com.school.management.domain.teaching.model.valueobject.ScheduleConflict;

import java.util.List;
import java.util.Optional;

/**
 * 课表仓储接口
 */
public interface CourseScheduleRepository {

    // ==================== 课表操作 ====================

    /**
     * 保存课表
     */
    CourseSchedule save(CourseSchedule schedule);

    /**
     * 根据ID查询课表
     */
    Optional<CourseSchedule> findById(Long id);

    /**
     * 根据ID查询课表（含条目）
     */
    Optional<CourseSchedule> findByIdWithEntries(Long id);

    /**
     * 根据学期ID查询课表
     */
    Optional<CourseSchedule> findBySemesterId(Long semesterId);

    /**
     * 根据学期ID查询课表（含条目）
     */
    Optional<CourseSchedule> findBySemesterIdWithEntries(Long semesterId);

    /**
     * 检查学期是否已有课表
     */
    boolean existsBySemesterId(Long semesterId);

    /**
     * 删除课表
     */
    void deleteById(Long id);

    // ==================== 排课条目操作 ====================

    /**
     * 保存排课条目
     */
    ScheduleEntry saveEntry(ScheduleEntry entry);

    /**
     * 批量保存排课条目
     */
    List<ScheduleEntry> saveEntries(List<ScheduleEntry> entries);

    /**
     * 根据ID查询排课条目
     */
    Optional<ScheduleEntry> findEntryById(Long id);

    /**
     * 根据课表ID查询所有条目
     */
    List<ScheduleEntry> findEntriesByScheduleId(Long scheduleId);

    /**
     * 查询学期的所有排课条目
     */
    List<ScheduleEntry> findEntriesBySemesterId(Long semesterId);

    /**
     * 查询班级课表
     */
    List<ScheduleEntry> findEntriesByClassId(Long semesterId, Long classId);

    /**
     * 查询教师课表
     */
    List<ScheduleEntry> findEntriesByTeacherId(Long semesterId, Long teacherId);

    /**
     * 查询教室课表
     */
    List<ScheduleEntry> findEntriesByClassroomId(Long semesterId, Long classroomId);

    /**
     * 查询任务的排课条目
     */
    List<ScheduleEntry> findEntriesByTaskId(Long taskId);

    /**
     * 删除排课条目
     */
    void deleteEntry(Long id);

    /**
     * 删除排课条目（别名方法）
     */
    default void deleteEntryById(Long id) {
        deleteEntry(id);
    }

    /**
     * 删除任务的所有排课
     */
    void deleteEntriesByTaskId(Long taskId);

    // ==================== 冲突检测 ====================

    /**
     * 保存冲突记录
     */
    ScheduleConflict saveConflict(ScheduleConflict conflict);

    /**
     * 查询未解决的冲突
     */
    List<ScheduleConflict> findUnresolvedConflicts(Long semesterId);

    /**
     * 查询条目相关的冲突
     */
    List<ScheduleConflict> findConflictsByEntryId(Long entryId);

    /**
     * 删除冲突记录
     */
    void deleteConflictById(Long id);

    /**
     * 检测冲突
     */
    List<ScheduleConflict> detectConflicts(ScheduleEntry entry);

    /**
     * 计算任务已排课时
     */
    int countScheduledHours(Long taskId);
}
