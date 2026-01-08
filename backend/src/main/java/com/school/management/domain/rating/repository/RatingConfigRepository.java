package com.school.management.domain.rating.repository;

import com.school.management.domain.rating.model.RatingConfig;
import com.school.management.domain.rating.model.RatingPeriodType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RatingConfig aggregate.
 */
public interface RatingConfigRepository {

    /**
     * Saves a rating configuration.
     *
     * @param config the config to save
     * @return the saved config
     */
    RatingConfig save(RatingConfig config);

    /**
     * Finds a config by ID.
     *
     * @param id the config ID
     * @return the config or empty
     */
    Optional<RatingConfig> findById(Long id);

    /**
     * Finds configs by check plan ID.
     *
     * @param checkPlanId the check plan ID
     * @return list of configs
     */
    List<RatingConfig> findByCheckPlanId(Long checkPlanId);

    /**
     * Finds enabled configs by check plan and period type.
     *
     * @param checkPlanId the check plan ID
     * @param periodType  the period type
     * @return list of enabled configs
     */
    List<RatingConfig> findEnabledByCheckPlanAndPeriodType(Long checkPlanId, RatingPeriodType periodType);

    /**
     * Finds all enabled configs.
     *
     * @return list of enabled configs
     */
    List<RatingConfig> findAllEnabled();

    /**
     * Deletes a config by ID.
     *
     * @param id the config ID
     */
    void deleteById(Long id);

    /**
     * Checks if a config exists.
     *
     * @param id the config ID
     * @return true if exists
     */
    boolean existsById(Long id);
}
