package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ClassInspectionRecord aggregate.
 */
public interface ClassInspectionRecordRepository extends Repository<ClassInspectionRecord, Long> {

    List<ClassInspectionRecord> findBySessionId(Long sessionId);

    Optional<ClassInspectionRecord> findBySessionIdAndClassId(Long sessionId, Long classId);

    int countBySessionId(Long sessionId);
}
