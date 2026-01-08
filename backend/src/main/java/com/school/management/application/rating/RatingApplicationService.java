package com.school.management.application.rating;

import com.school.management.domain.rating.model.*;
import com.school.management.domain.rating.repository.RatingConfigRepository;
import com.school.management.domain.rating.repository.RatingResultRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Application service for Rating management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingApplicationService {

    private final RatingConfigRepository configRepository;
    private final RatingResultRepository resultRepository;
    private final DomainEventPublisher eventPublisher;

    // ========== Rating Config Operations ==========

    /**
     * Creates a new rating configuration.
     */
    @Transactional
    public RatingConfig createConfig(CreateRatingConfigCommand command) {
        log.info("Creating rating config: {}", command.getRatingName());

        RatingConfig config = RatingConfig.create(
            command.getCheckPlanId(),
            command.getRatingName(),
            command.getPeriodType(),
            command.getDivisionMethod(),
            command.getDivisionValue(),
            command.getCreatedBy()
        );

        if (command.getIcon() != null || command.getColor() != null) {
            config.setAppearance(command.getIcon(), command.getColor(), command.getPriority());
        }

        if (command.getDescription() != null) {
            config.update(null, null, null, null, command.getDescription());
        }

        config.configureApproval(command.isRequireApproval(), command.isAutoPublish());

        return configRepository.save(config);
    }

    /**
     * Updates a rating configuration.
     */
    @Transactional
    public RatingConfig updateConfig(Long configId, UpdateRatingConfigCommand command) {
        RatingConfig config = getConfigOrThrow(configId);

        config.update(
            command.getRatingName(),
            command.getPeriodType(),
            command.getDivisionMethod(),
            command.getDivisionValue(),
            command.getDescription()
        );

        if (command.getIcon() != null || command.getColor() != null) {
            config.setAppearance(command.getIcon(), command.getColor(), command.getPriority());
        }

        if (command.getRequireApproval() != null || command.getAutoPublish() != null) {
            config.configureApproval(
                command.getRequireApproval() != null ? command.getRequireApproval() : config.isRequireApproval(),
                command.getAutoPublish() != null ? command.getAutoPublish() : config.isAutoPublish()
            );
        }

        return configRepository.save(config);
    }

    /**
     * Toggles config enabled status.
     */
    @Transactional
    public RatingConfig toggleConfigEnabled(Long configId, boolean enabled) {
        RatingConfig config = getConfigOrThrow(configId);
        config.setEnabled(enabled);
        return configRepository.save(config);
    }

    /**
     * Deletes a config.
     */
    @Transactional
    public void deleteConfig(Long configId) {
        if (!configRepository.existsById(configId)) {
            throw new BusinessException("Rating config not found: " + configId);
        }
        configRepository.deleteById(configId);
    }

    /**
     * Gets a config by ID.
     */
    @Transactional(readOnly = true)
    public Optional<RatingConfig> getConfig(Long configId) {
        return configRepository.findById(configId);
    }

    /**
     * Gets configs by check plan.
     */
    @Transactional(readOnly = true)
    public List<RatingConfig> getConfigsByCheckPlan(Long checkPlanId) {
        return configRepository.findByCheckPlanId(checkPlanId);
    }

    // ========== Rating Result Operations ==========

    /**
     * Approves a rating result.
     */
    @Transactional
    public RatingResult approveResult(Long resultId, Long approverId, String comment) {
        RatingResult result = getResultOrThrow(resultId);
        result.approve(approverId, comment);

        result = resultRepository.save(result);
        publishEvents(result);

        // Check if auto-publish is enabled
        RatingConfig config = configRepository.findById(result.getRatingConfigId()).orElse(null);
        if (config != null && config.isAutoPublish()) {
            result.publish(approverId);
            result = resultRepository.save(result);
            publishEvents(result);
        }

        return result;
    }

    /**
     * Batch approves rating results.
     */
    @Transactional
    public List<RatingResult> batchApproveResults(List<Long> resultIds, Long approverId, String comment) {
        return resultIds.stream()
            .map(id -> approveResult(id, approverId, comment))
            .toList();
    }

    /**
     * Rejects a rating result.
     */
    @Transactional
    public RatingResult rejectResult(Long resultId, Long reviewerId, String reason) {
        RatingResult result = getResultOrThrow(resultId);
        result.reject(reviewerId, reason);

        result = resultRepository.save(result);
        publishEvents(result);

        return result;
    }

    /**
     * Publishes a rating result.
     */
    @Transactional
    public RatingResult publishResult(Long resultId, Long publisherId) {
        RatingResult result = getResultOrThrow(resultId);
        result.publish(publisherId);

        result = resultRepository.save(result);
        publishEvents(result);

        return result;
    }

    /**
     * Batch publishes rating results.
     */
    @Transactional
    public List<RatingResult> batchPublishResults(List<Long> resultIds, Long publisherId) {
        return resultIds.stream()
            .map(id -> publishResult(id, publisherId))
            .toList();
    }

    /**
     * Revokes a published result.
     */
    @Transactional
    public RatingResult revokeResult(Long resultId, Long revokedBy) {
        RatingResult result = getResultOrThrow(resultId);
        result.revoke(revokedBy);

        result = resultRepository.save(result);
        publishEvents(result);

        return result;
    }

    /**
     * Gets a result by ID.
     */
    @Transactional(readOnly = true)
    public Optional<RatingResult> getResult(Long resultId) {
        return resultRepository.findById(resultId);
    }

    /**
     * Gets results pending approval.
     */
    @Transactional(readOnly = true)
    public List<RatingResult> getPendingApprovalResults() {
        return resultRepository.findPendingApproval();
    }

    /**
     * Gets results by class ID.
     */
    @Transactional(readOnly = true)
    public List<RatingResult> getResultsByClass(Long classId) {
        return resultRepository.findByClassId(classId);
    }

    /**
     * Gets results by config ID.
     */
    @Transactional(readOnly = true)
    public List<RatingResult> getResultsByConfig(Long ratingConfigId) {
        return resultRepository.findByRatingConfigId(ratingConfigId);
    }

    // ========== Helper Methods ==========

    private RatingConfig getConfigOrThrow(Long configId) {
        return configRepository.findById(configId)
            .orElseThrow(() -> new BusinessException("Rating config not found: " + configId));
    }

    private RatingResult getResultOrThrow(Long resultId) {
        return resultRepository.findById(resultId)
            .orElseThrow(() -> new BusinessException("Rating result not found: " + resultId));
    }

    private void publishEvents(RatingResult result) {
        result.getDomainEvents().forEach(eventPublisher::publish);
        result.clearDomainEvents();
    }
}
