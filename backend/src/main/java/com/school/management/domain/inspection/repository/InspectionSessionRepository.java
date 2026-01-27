package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.InspectionSession;
import com.school.management.domain.inspection.model.SessionStatus;
import com.school.management.domain.shared.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for InspectionSession aggregate.
 */
public interface InspectionSessionRepository extends Repository<InspectionSession, Long> {

    Optional<InspectionSession> findBySessionCode(String sessionCode);

    List<InspectionSession> findByStatus(SessionStatus status);

    List<InspectionSession> findByInspectionDate(LocalDate date);

    List<InspectionSession> findByDateRange(LocalDate startDate, LocalDate endDate);

    List<InspectionSession> findByInspectorId(Long inspectorId);

    boolean existsBySessionCode(String sessionCode);
}
