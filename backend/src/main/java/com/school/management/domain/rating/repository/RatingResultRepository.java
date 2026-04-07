package com.school.management.domain.rating.repository;

import com.school.management.domain.rating.model.RatingResult;
import com.school.management.domain.rating.model.RatingResultStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RatingResult aggregate.
 */
public interface RatingResultRepository {

    /**
     * Saves a rating result.
     *
     * @param result the result to save
     * @return the saved result
     */
    RatingResult save(RatingResult result);

    /**
     * Saves multiple rating results.
     *
     * @param results the results to save
     * @return the saved results
     */
    List<RatingResult> saveAll(List<RatingResult> results);

    /**
     * Finds a result by ID.
     *
     * @param id the result ID
     * @return the result or empty
     */
    Optional<RatingResult> findById(Long id);

    /**
     * Finds results by rating config ID.
     *
     * @param ratingConfigId the rating config ID
     * @return list of results
     */
    List<RatingResult> findByRatingConfigId(Long ratingConfigId);

    /**
     * Finds results by class ID.
     *
     * @param classId the class ID
     * @return list of results
     */
    List<RatingResult> findByClassId(Long orgUnitId);

    /**
     * Finds results by status.
     *
     * @param status the result status
     * @return list of results
     */
    List<RatingResult> findByStatus(RatingResultStatus status);

    /**
     * Finds results by config and period.
     *
     * @param ratingConfigId the rating config ID
     * @param periodStart    period start date
     * @param periodEnd      period end date
     * @return list of results
     */
    List<RatingResult> findByConfigAndPeriod(Long ratingConfigId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * Finds results pending approval.
     *
     * @return list of pending results
     */
    List<RatingResult> findPendingApproval();

    /**
     * Counts results by status.
     *
     * @param status the result status
     * @return count of results
     */
    long countByStatus(RatingResultStatus status);

    /**
     * Deletes a result by ID.
     *
     * @param id the result ID
     */
    void deleteById(Long id);

    /**
     * Checks if a result exists.
     *
     * @param id the result ID
     * @return true if exists
     */
    boolean existsById(Long id);
}
