package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.scheduling.ScheduleConflictRecord;
import java.util.List;

public interface ScheduleConflictRecordRepository {
    ScheduleConflictRecord save(ScheduleConflictRecord record);
    List<ScheduleConflictRecord> findBySemesterId(Long semesterId);
    List<ScheduleConflictRecord> findByDetectionBatch(String batch);
    List<ScheduleConflictRecord> findBySemesterIdAndStatus(Long semesterId, Integer status);
    void deleteByDetectionBatch(String batch);
}
