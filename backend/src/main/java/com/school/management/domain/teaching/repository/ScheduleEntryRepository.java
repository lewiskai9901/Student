package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.scheduling.ScheduleEntry;
import java.util.List;
import java.util.Optional;

public interface ScheduleEntryRepository {
    ScheduleEntry save(ScheduleEntry entry);
    Optional<ScheduleEntry> findById(Long id);
    List<ScheduleEntry> findBySemesterId(Long semesterId);
    List<ScheduleEntry> findByTaskId(Long taskId);
    List<ScheduleEntry> findByTeacherAndWeekday(Long semesterId, Long teacherId, Integer weekday);
    List<ScheduleEntry> findByClassroomAndWeekday(Long semesterId, Long classroomId, Integer weekday);
    List<ScheduleEntry> findByClassAndWeekday(Long semesterId, Long orgUnitId, Integer weekday);
    void deleteById(Long id);
    void deleteByTaskId(Long taskId);
}
