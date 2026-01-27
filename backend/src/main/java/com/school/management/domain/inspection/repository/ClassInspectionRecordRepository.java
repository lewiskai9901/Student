package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import com.school.management.domain.shared.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ClassInspectionRecord aggregate.
 */
public interface ClassInspectionRecordRepository extends Repository<ClassInspectionRecord, Long> {

    List<ClassInspectionRecord> findBySessionId(Long sessionId);

    Optional<ClassInspectionRecord> findBySessionIdAndClassId(Long sessionId, Long classId);

    int countBySessionId(Long sessionId);

    List<ClassInspectionRecord> findByClassIdAndDateRange(Long classId, LocalDate start, LocalDate end);

    List<ClassInspectionRecord> findByClassIdOrderByCreatedAtDesc(Long classId, int limit);
}
