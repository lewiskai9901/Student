package com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository;

import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.scheduling.ScheduleConflictRecord;
import java.util.List;
import java.util.Optional;

public interface ScheduleConflictRecordRepository {
    ScheduleConflictRecord save(ScheduleConflictRecord record);
    Optional<ScheduleConflictRecord> findById(Long id);
    List<ScheduleConflictRecord> findBySemesterId(Long semesterId);
    List<ScheduleConflictRecord> findByDetectionBatch(String batch);
    List<ScheduleConflictRecord> findBySemesterIdAndStatus(Long semesterId, Integer status);
    void deleteByDetectionBatch(String batch);
}
