package com.school.management.domain.behavior.repository;

import com.school.management.domain.behavior.model.BehaviorRecord;
import com.school.management.domain.behavior.model.BehaviorType;
import com.school.management.domain.shared.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for BehaviorRecord aggregate.
 */
public interface BehaviorRecordRepository extends Repository<BehaviorRecord, Long> {

    List<BehaviorRecord> findByStudentId(Long studentId);

    List<BehaviorRecord> findByClassId(Long classId);

    List<BehaviorRecord> findByClassIdAndDateRange(Long classId, LocalDateTime start, LocalDateTime end);

    long countByStudentIdAndType(Long studentId, BehaviorType type);

    List<BehaviorRecord> findByStudentIdAndDateRange(Long studentId, LocalDateTime start, LocalDateTime end);
}
