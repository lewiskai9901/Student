package com.school.management.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Feature toggle configuration for gradual V2 API rollout.
 *
 * <p>Supports:
 * <ul>
 *   <li>Per-feature enable/disable</li>
 *   <li>Percentage-based rollout</li>
 *   <li>User whitelist for early access</li>
 * </ul>
 *
 * <p>Configuration example:
 * <pre>
 * feature:
 *   toggles:
 *     v2-rating-api:
 *       enabled: true
 *       rollout-percentage: 50
 *     v2-task-api:
 *       enabled: true
 *       whitelist-users: [1, 2, 3]
 * </pre>
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "feature")
public class FeatureToggleConfig {

    /**
     * Feature toggle definitions.
     */
    private Map<String, FeatureToggle> toggles = new HashMap<>();

    // ========== Feature Names ==========

    public static final String V2_RATING_API = "v2-rating-api";
    public static final String V2_TASK_API = "v2-task-api";
    public static final String V2_INSPECTION_API = "v2-inspection-api";
    public static final String V2_ORGANIZATION_API = "v2-organization-api";
    public static final String CQRS_READ_MODEL = "cqrs-read-model";
    public static final String PERFORMANCE_MONITORING = "performance-monitoring";
    public static final String EVENT_SOURCING = "event-sourcing";

    @PostConstruct
    public void init() {
        // Initialize default toggles if not configured
        initDefaultToggle(V2_RATING_API, true, 100);
        initDefaultToggle(V2_TASK_API, true, 100);
        initDefaultToggle(V2_INSPECTION_API, true, 100);
        initDefaultToggle(V2_ORGANIZATION_API, true, 100);
        initDefaultToggle(CQRS_READ_MODEL, true, 100);
        initDefaultToggle(PERFORMANCE_MONITORING, true, 100);
        initDefaultToggle(EVENT_SOURCING, false, 0);

        log.info("Feature toggles initialized: {}", toggles.keySet());
    }

    private void initDefaultToggle(String name, boolean enabled, int rolloutPercentage) {
        toggles.computeIfAbsent(name, k -> {
            FeatureToggle toggle = new FeatureToggle();
            toggle.setEnabled(enabled);
            toggle.setRolloutPercentage(rolloutPercentage);
            return toggle;
        });
    }

    /**
     * Checks if a feature is enabled for a user.
     *
     * @param featureName feature name
     * @param userId      user ID (can be null for anonymous)
     * @return true if enabled
     */
    public boolean isEnabled(String featureName, Long userId) {
        FeatureToggle toggle = toggles.get(featureName);
        if (toggle == null) {
            log.warn("Unknown feature toggle: {}", featureName);
            return false;
        }

        if (!toggle.isEnabled()) {
            return false;
        }

        // Check whitelist first
        if (toggle.getWhitelistUsers() != null && userId != null) {
            if (toggle.getWhitelistUsers().contains(userId)) {
                return true;
            }
        }

        // Check blacklist
        if (toggle.getBlacklistUsers() != null && userId != null) {
            if (toggle.getBlacklistUsers().contains(userId)) {
                return false;
            }
        }

        // Check rollout percentage
        if (toggle.getRolloutPercentage() >= 100) {
            return true;
        }

        if (toggle.getRolloutPercentage() <= 0) {
            return false;
        }

        // Use user ID for consistent rollout assignment
        if (userId != null) {
            int bucket = (int) (userId % 100);
            return bucket < toggle.getRolloutPercentage();
        }

        // Random for anonymous users
        return Math.random() * 100 < toggle.getRolloutPercentage();
    }

    /**
     * Checks if a feature is globally enabled.
     *
     * @param featureName feature name
     * @return true if enabled
     */
    public boolean isEnabled(String featureName) {
        return isEnabled(featureName, null);
    }

    /**
     * Dynamically enables a feature.
     *
     * @param featureName feature name
     */
    public void enableFeature(String featureName) {
        FeatureToggle toggle = toggles.get(featureName);
        if (toggle != null) {
            toggle.setEnabled(true);
            toggle.setRolloutPercentage(100);
            log.info("Feature enabled: {}", featureName);
        }
    }

    /**
     * Dynamically disables a feature.
     *
     * @param featureName feature name
     */
    public void disableFeature(String featureName) {
        FeatureToggle toggle = toggles.get(featureName);
        if (toggle != null) {
            toggle.setEnabled(false);
            log.info("Feature disabled: {}", featureName);
        }
    }

    /**
     * Sets rollout percentage for gradual rollout.
     *
     * @param featureName feature name
     * @param percentage  0-100
     */
    public void setRolloutPercentage(String featureName, int percentage) {
        FeatureToggle toggle = toggles.get(featureName);
        if (toggle != null) {
            toggle.setRolloutPercentage(Math.max(0, Math.min(100, percentage)));
            log.info("Feature {} rollout set to {}%", featureName, percentage);
        }
    }

    /**
     * Feature toggle definition.
     */
    @Data
    public static class FeatureToggle {
        private boolean enabled = true;
        private int rolloutPercentage = 100;
        private Set<Long> whitelistUsers;
        private Set<Long> blacklistUsers;
        private String description;
    }
}
