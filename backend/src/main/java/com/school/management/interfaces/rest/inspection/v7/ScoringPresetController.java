package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.ScoringPresetApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.execution.ScoringPreset;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScoringPresetController {

    private final ScoringPresetApplicationService scoringPresetService;

    @GetMapping("/v7/insp/templates/{templateId}/scoring-presets")
    @CasbinAccess(resource = "insp:execution", action = "view")
    public Result<List<ScoringPreset>> listPresets(@PathVariable Long templateId) {
        return Result.success(scoringPresetService.listByTemplateId(templateId));
    }

    @PostMapping("/v7/insp/templates/{templateId}/scoring-presets")
    @CasbinAccess(resource = "insp:execution", action = "edit")
    public Result<ScoringPreset> createPreset(@PathVariable Long templateId,
                                               @RequestBody CreateScoringPresetRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(scoringPresetService.createPreset(
                templateId, request.getPresetName(), request.getPresetType(),
                request.getItemValues(), userId));
    }

    @PutMapping("/v7/insp/scoring-presets/{id}")
    @CasbinAccess(resource = "insp:execution", action = "edit")
    public Result<ScoringPreset> updatePreset(@PathVariable Long id,
                                               @RequestBody UpdateScoringPresetRequest request) {
        return Result.success(scoringPresetService.updatePreset(
                id, request.getPresetName(), request.getItemValues()));
    }

    @PostMapping("/v7/insp/scoring-presets/{id}/apply")
    @CasbinAccess(resource = "insp:execution", action = "edit")
    public Result<ScoringPreset> applyPreset(@PathVariable Long id) {
        return Result.success(scoringPresetService.applyPreset(id));
    }

    @DeleteMapping("/v7/insp/scoring-presets/{id}")
    @CasbinAccess(resource = "insp:execution", action = "edit")
    public Result<Void> deletePreset(@PathVariable Long id) {
        scoringPresetService.deletePreset(id);
        return Result.success();
    }

    @lombok.Data
    public static class CreateScoringPresetRequest {
        private String presetName;
        private String presetType;
        private String itemValues;
    }

    @lombok.Data
    public static class UpdateScoringPresetRequest {
        private String presetName;
        private String itemValues;
    }
}
