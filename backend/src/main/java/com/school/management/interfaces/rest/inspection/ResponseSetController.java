package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.ResponseSetApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.template.ResponseSet;
import com.school.management.domain.inspection.model.template.ResponseSetOption;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.school.management.common.PageResult;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/inspection/response-sets")
@RequiredArgsConstructor
@Validated
public class ResponseSetController {

    private final ResponseSetApplicationService responseSetService;

    @PostMapping
    @CasbinAccess(resource = "insp:response-set", action = "create")
    public Result<ResponseSet> createResponseSet(@RequestBody @Valid CreateResponseSetRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(responseSetService.createResponseSet(
                request.getSetCode(), request.getSetName(), request.getIsGlobal(), userId));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:response-set", action = "view")
    public Result<PageResult<ResponseSet>> listResponseSets(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(responseSetService.listResponseSets(page, size, keyword));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:response-set", action = "view")
    public Result<ResponseSet> getResponseSet(@PathVariable Long id) {
        return Result.success(responseSetService.getResponseSet(id)
                .orElseThrow(() -> new IllegalArgumentException("选项集不存在: " + id)));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:response-set", action = "edit")
    public Result<ResponseSet> updateResponseSet(@PathVariable Long id,
                                                  @RequestBody @Valid UpdateResponseSetRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(responseSetService.updateResponseSet(
                id, request.getSetName(), request.getIsGlobal(), request.getIsEnabled(), userId));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:response-set", action = "delete")
    public Result<Void> deleteResponseSet(@PathVariable Long id) {
        responseSetService.deleteResponseSet(id);
        return Result.success();
    }

    // --- Options ---

    @PostMapping("/{id}/options")
    @CasbinAccess(resource = "insp:response-set", action = "edit")
    public Result<ResponseSetOption> addOption(@PathVariable Long id,
                                                @RequestBody @Valid CreateOptionRequest request) {
        return Result.success(responseSetService.addOption(
                id, request.getOptionValue(), request.getOptionLabel(),
                request.getOptionColor(), request.getScore(),
                request.getIsFlagged(), request.getSortOrder()));
    }

    @GetMapping("/{id}/options")
    @CasbinAccess(resource = "insp:response-set", action = "view")
    public Result<List<ResponseSetOption>> listOptions(@PathVariable Long id) {
        return Result.success(responseSetService.listOptions(id));
    }

    @PutMapping("/{id}/options/{optionId}")
    @CasbinAccess(resource = "insp:response-set", action = "edit")
    public Result<ResponseSetOption> updateOption(@PathVariable Long id,
                                                   @PathVariable Long optionId,
                                                   @RequestBody @Valid UpdateOptionRequest request) {
        return Result.success(responseSetService.updateOption(
                optionId, request.getOptionLabel(), request.getOptionColor(),
                request.getScore(), request.getIsFlagged(), request.getSortOrder()));
    }

    @DeleteMapping("/{id}/options/{optionId}")
    @CasbinAccess(resource = "insp:response-set", action = "edit")
    public Result<Void> deleteOption(@PathVariable Long id, @PathVariable Long optionId) {
        responseSetService.deleteOption(optionId);
        return Result.success();
    }

    @lombok.Data
    public static class CreateResponseSetRequest {
        @NotBlank
        private String setCode;
        @NotBlank
        private String setName;
        private Boolean isGlobal;
    }

    @lombok.Data
    public static class UpdateResponseSetRequest {
        @NotBlank
        private String setName;
        private Boolean isGlobal;
        private Boolean isEnabled;
    }

    @lombok.Data
    public static class CreateOptionRequest {
        @NotBlank
        private String optionValue;
        @NotBlank
        private String optionLabel;
        private String optionColor;
        private BigDecimal score;
        private Boolean isFlagged;
        private Integer sortOrder;
    }

    @lombok.Data
    public static class UpdateOptionRequest {
        private String optionLabel;
        private String optionColor;
        private BigDecimal score;
        private Boolean isFlagged;
        private Integer sortOrder;
    }
}
