package com.school.management.domain.teaching.service.impl;

import com.school.management.domain.teaching.model.entity.ScheduleEntry;
import com.school.management.domain.teaching.model.valueobject.ScheduleConflict;
import com.school.management.domain.teaching.service.ConflictDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 冲突检测领域服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConflictDetectionServiceImpl implements ConflictDetectionService {

    @Override
    public List<ScheduleConflict> detectConflicts(Long taskId, Integer weekday, Integer slot,
                                                   Integer startWeek, Integer endWeek,
                                                   Integer weekType, Long classroomId) {
        // TODO: 实现冲突检测逻辑
        // 目前返回空列表，表示无冲突
        log.debug("检测冲突: taskId={}, weekday={}, slot={}, weeks={}-{}, weekType={}, classroomId={}",
                taskId, weekday, slot, startWeek, endWeek, weekType, classroomId);
        return new ArrayList<>();
    }

    @Override
    public List<ScheduleConflict> detectAll(ScheduleEntry entry, List<ScheduleEntry> existingEntries) {
        List<ScheduleConflict> conflicts = new ArrayList<>();
        if (entry == null || existingEntries == null) {
            return conflicts;
        }

        for (ScheduleEntry existing : existingEntries) {
            if (existing.getId() != null && existing.getId().equals(entry.getId())) {
                continue; // 跳过自身
            }
            conflicts.addAll(detectBetween(entry, existing));
        }
        return conflicts;
    }

    @Override
    public List<ScheduleConflict> detectTeacherConflicts(ScheduleEntry entry, List<ScheduleEntry> existingEntries) {
        List<ScheduleConflict> conflicts = new ArrayList<>();
        if (entry == null || entry.getTeacherId() == null || existingEntries == null) {
            return conflicts;
        }

        for (ScheduleEntry existing : existingEntries) {
            if (existing.getId() != null && existing.getId().equals(entry.getId())) {
                continue;
            }
            if (entry.getTeacherId().equals(existing.getTeacherId())
                    && isTimeOverlap(entry, existing)
                    && isWeekOverlap(entry, existing)) {
                conflicts.add(ScheduleConflict.teacherConflict(entry.getId(), existing.getId(), entry.getTeacherId()));
            }
        }
        return conflicts;
    }

    @Override
    public List<ScheduleConflict> detectClassroomConflicts(ScheduleEntry entry, List<ScheduleEntry> existingEntries) {
        List<ScheduleConflict> conflicts = new ArrayList<>();
        if (entry == null || entry.getClassroomId() == null || existingEntries == null) {
            return conflicts;
        }

        for (ScheduleEntry existing : existingEntries) {
            if (existing.getId() != null && existing.getId().equals(entry.getId())) {
                continue;
            }
            if (entry.getClassroomId().equals(existing.getClassroomId())
                    && isTimeOverlap(entry, existing)
                    && isWeekOverlap(entry, existing)) {
                conflicts.add(ScheduleConflict.classroomConflict(entry.getId(), existing.getId(), entry.getClassroomId()));
            }
        }
        return conflicts;
    }

    @Override
    public List<ScheduleConflict> detectClassConflicts(ScheduleEntry entry, List<ScheduleEntry> existingEntries) {
        List<ScheduleConflict> conflicts = new ArrayList<>();
        if (entry == null || entry.getClassId() == null || existingEntries == null) {
            return conflicts;
        }

        for (ScheduleEntry existing : existingEntries) {
            if (existing.getId() != null && existing.getId().equals(entry.getId())) {
                continue;
            }
            if (entry.getClassId().equals(existing.getClassId())
                    && isTimeOverlap(entry, existing)
                    && isWeekOverlap(entry, existing)) {
                conflicts.add(ScheduleConflict.classConflict(entry.getId(), existing.getId(), entry.getClassId()));
            }
        }
        return conflicts;
    }

    @Override
    public List<ScheduleConflict> detectBetween(ScheduleEntry entry1, ScheduleEntry entry2) {
        List<ScheduleConflict> conflicts = new ArrayList<>();
        if (entry1 == null || entry2 == null) {
            return conflicts;
        }

        if (!isTimeOverlap(entry1, entry2) || !isWeekOverlap(entry1, entry2)) {
            return conflicts;
        }

        // 教师冲突
        if (entry1.getTeacherId() != null && entry1.getTeacherId().equals(entry2.getTeacherId())) {
            conflicts.add(ScheduleConflict.teacherConflict(entry1.getId(), entry2.getId(), entry1.getTeacherId()));
        }

        // 教室冲突
        if (entry1.getClassroomId() != null && entry1.getClassroomId().equals(entry2.getClassroomId())) {
            conflicts.add(ScheduleConflict.classroomConflict(entry1.getId(), entry2.getId(), entry1.getClassroomId()));
        }

        // 班级冲突
        if (entry1.getClassId() != null && entry1.getClassId().equals(entry2.getClassId())) {
            conflicts.add(ScheduleConflict.classConflict(entry1.getId(), entry2.getId(), entry1.getClassId()));
        }

        return conflicts;
    }

    @Override
    public boolean isTimeOverlap(ScheduleEntry entry1, ScheduleEntry entry2) {
        if (entry1.getWeekday() == null || entry2.getWeekday() == null) {
            return false;
        }
        if (!entry1.getWeekday().equals(entry2.getWeekday())) {
            return false;
        }

        Integer start1 = entry1.getStartSlot() != null ? entry1.getStartSlot() : entry1.getSlot();
        Integer end1 = entry1.getEndSlot() != null ? entry1.getEndSlot() : entry1.getSlot();
        Integer start2 = entry2.getStartSlot() != null ? entry2.getStartSlot() : entry2.getSlot();
        Integer end2 = entry2.getEndSlot() != null ? entry2.getEndSlot() : entry2.getSlot();

        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }

        return !(end1 < start2 || start1 > end2);
    }

    @Override
    public boolean isWeekOverlap(ScheduleEntry entry1, ScheduleEntry entry2) {
        Integer startWeek1 = entry1.getStartWeek();
        Integer endWeek1 = entry1.getEndWeek();
        Integer startWeek2 = entry2.getStartWeek();
        Integer endWeek2 = entry2.getEndWeek();

        if (startWeek1 == null || endWeek1 == null || startWeek2 == null || endWeek2 == null) {
            return true; // 缺少信息时假设可能冲突
        }

        // 检查周次范围
        if (endWeek1 < startWeek2 || startWeek1 > endWeek2) {
            return false;
        }

        // 检查单双周
        Integer weekType1 = entry1.getWeekType() != null ? entry1.getWeekType() : 0;
        Integer weekType2 = entry2.getWeekType() != null ? entry2.getWeekType() : 0;
        if (weekType1 != 0 && weekType2 != 0 && !weekType1.equals(weekType2)) {
            return false; // 一个单周一个双周不冲突
        }

        return true;
    }
}
