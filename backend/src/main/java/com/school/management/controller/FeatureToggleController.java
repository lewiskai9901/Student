package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.config.FeatureToggleConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing feature toggles.
 */
@RestController
@RequestMapping("/v2/features")
@RequiredArgsConstructor
@Tag(name = "Feature Toggles", description = "Feature toggle management API")
public class FeatureToggleController {

    private final FeatureToggleConfig featureToggleConfig;

    @GetMapping
    @Operation(summary = "Get all feature toggles")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, FeatureStatus>> getAllFeatures() {
        Map<String, FeatureStatus> features = featureToggleConfig.getToggles().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    FeatureStatus status = new FeatureStatus();
                    status.setEnabled(e.getValue().isEnabled());
                    status.setRolloutPercentage(e.getValue().getRolloutPercentage());
                    status.setDescription(e.getValue().getDescription());
                    return status;
                }
            ));

        return Result.success(features);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Check if a feature is enabled")
    public Result<Boolean> isFeatureEnabled(
            @PathVariable String name,
            @RequestParam(required = false) Long userId) {

        boolean enabled = featureToggleConfig.isEnabled(name, userId);
        return Result.success(enabled);
    }

    @PostMapping("/{name}/enable")
    @Operation(summary = "Enable a feature")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> enableFeature(@PathVariable String name) {
        featureToggleConfig.enableFeature(name);
        return Result.success();
    }

    @PostMapping("/{name}/disable")
    @Operation(summary = "Disable a feature")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> disableFeature(@PathVariable String name) {
        featureToggleConfig.disableFeature(name);
        return Result.success();
    }

    @PostMapping("/{name}/rollout")
    @Operation(summary = "Set rollout percentage")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> setRollout(
            @PathVariable String name,
            @RequestParam int percentage) {

        featureToggleConfig.setRolloutPercentage(name, percentage);
        return Result.success();
    }

    @Data
    public static class FeatureStatus {
        private boolean enabled;
        private int rolloutPercentage;
        private String description;
    }
}
