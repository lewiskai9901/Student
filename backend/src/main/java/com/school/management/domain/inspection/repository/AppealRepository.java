package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.Appeal;
import com.school.management.domain.inspection.model.AppealStatus;
import com.school.management.domain.shared.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Appeal aggregate.
 */
public interface AppealRepository extends Repository<Appeal, Long> {

    /**
     * Finds an appeal by its code.
     */
    Optional<Appeal> findByAppealCode(String appealCode);

    /**
     * Finds appeals by status.
     */
    List<Appeal> findByStatus(AppealStatus status);

    /**
     * Finds appeals by class ID.
     */
    List<Appeal> findByClassId(Long classId);

    /**
     * Finds appeals by applicant.
     */
    List<Appeal> findByApplicantId(Long applicantId);

    /**
     * Finds appeals by inspection record.
     */
    List<Appeal> findByInspectionRecordId(Long recordId);

    /**
     * Finds appeals pending Level 1 review.
     */
    List<Appeal> findPendingLevel1Review();

    /**
     * Finds appeals pending Level 2 review.
     */
    List<Appeal> findPendingLevel2Review();

    /**
     * Finds appeals assigned to a reviewer.
     */
    List<Appeal> findByLevel1ReviewerId(Long reviewerId);

    /**
     * Finds appeals by date range.
     */
    List<Appeal> findByAppliedDateRange(LocalDateTime start, LocalDateTime end);

    /**
     * Counts appeals by status.
     */
    long countByStatus(AppealStatus status);

    /**
     * Checks if an appeal already exists for a deduction detail.
     */
    boolean existsByDeductionDetailId(Long deductionDetailId);

    /**
     * Finds all appeals that are approved but not yet effective.
     */
    List<Appeal> findApprovedNotEffective();
}
