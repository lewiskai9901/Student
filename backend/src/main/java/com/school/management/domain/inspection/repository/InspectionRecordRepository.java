package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.InspectionRecord;
import com.school.management.domain.inspection.model.RecordStatus;
import com.school.management.domain.shared.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for InspectionRecord aggregate.
 */
public interface InspectionRecordRepository extends Repository<InspectionRecord, Long> {

    /**
     * Finds a record by its code.
     */
    Optional<InspectionRecord> findByRecordCode(String recordCode);

    /**
     * Finds records by inspection date.
     */
    List<InspectionRecord> findByInspectionDate(LocalDate date);

    /**
     * Finds records by date range.
     */
    List<InspectionRecord> findByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Finds records by status.
     */
    List<InspectionRecord> findByStatus(RecordStatus status);

    /**
     * Finds records by template ID.
     */
    List<InspectionRecord> findByTemplateId(Long templateId);

    /**
     * Finds records by inspector.
     */
    List<InspectionRecord> findByInspectorId(Long inspectorId);

    /**
     * Finds records pending review.
     */
    List<InspectionRecord> findPendingReview();

    /**
     * Checks if a record code already exists.
     */
    boolean existsByRecordCode(String recordCode);

    /**
     * Finds the latest record for a specific date and period.
     */
    Optional<InspectionRecord> findLatestByDateAndPeriod(LocalDate date, String period);

    /**
     * Counts records by status.
     */
    long countByStatus(RecordStatus status);
}
