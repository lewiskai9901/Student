package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.BonusItemApplicationService;
import com.school.management.application.inspection.command.CreateBonusItemCommand;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.BonusItem;
import com.school.management.domain.inspection.model.BonusMode;
import com.school.management.interfaces.rest.inspection.dto.BonusItemResponse;
import com.school.management.interfaces.rest.inspection.dto.CreateBonusItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for bonus item management.
 */
@RestController
@RequestMapping("/inspection/bonus-items")
@Tag(name = "Bonus Items", description = "Bonus item configuration API")
public class BonusItemController {

    private final BonusItemApplicationService bonusItemService;

    public BonusItemController(BonusItemApplicationService bonusItemService) {
        this.bonusItemService = bonusItemService;
    }

    @GetMapping
    @Operation(summary = "List all enabled bonus items")
    @PreAuthorize("hasAuthority('inspection:template:view')")
    public Result<List<BonusItemResponse>> listAll() {
        List<BonusItemResponse> items = bonusItemService.listAllEnabled().stream()
            .map(BonusItemResponse::fromDomain)
            .collect(Collectors.toList());
        return Result.success(items);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "List bonus items by category")
    @PreAuthorize("hasAuthority('inspection:template:view')")
    public Result<List<BonusItemResponse>> listByCategory(@PathVariable Long categoryId) {
        List<BonusItemResponse> items = bonusItemService.listByCategoryId(categoryId).stream()
            .map(BonusItemResponse::fromDomain)
            .collect(Collectors.toList());
        return Result.success(items);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bonus item by ID")
    @PreAuthorize("hasAuthority('inspection:template:view')")
    public Result<BonusItemResponse> getById(@PathVariable Long id) {
        return bonusItemService.getBonusItem(id)
            .map(item -> Result.success(BonusItemResponse.fromDomain(item)))
            .orElse(Result.error("Bonus item not found"));
    }

    @PostMapping
    @Operation(summary = "Create a new bonus item")
    @PreAuthorize("hasAuthority('inspection:template:edit')")
    public Result<BonusItemResponse> create(@Valid @RequestBody CreateBonusItemRequest request) {
        CreateBonusItemCommand command = CreateBonusItemCommand.builder()
            .categoryId(request.getCategoryId())
            .itemName(request.getItemName())
            .bonusMode(BonusMode.valueOf(request.getBonusMode()))
            .fixedBonus(request.getFixedBonus())
            .progressiveConfig(request.getProgressiveConfig())
            .improvementCoefficient(request.getImprovementCoefficient())
            .description(request.getDescription())
            .sortOrder(request.getSortOrder())
            .build();

        BonusItem item = bonusItemService.createBonusItem(command);
        return Result.success(BonusItemResponse.fromDomain(item));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a bonus item")
    @PreAuthorize("hasAuthority('inspection:template:edit')")
    public Result<BonusItemResponse> update(@PathVariable Long id, @Valid @RequestBody CreateBonusItemRequest request) {
        BonusItem item = bonusItemService.updateBonusItem(
            id,
            request.getItemName(),
            BonusMode.valueOf(request.getBonusMode()),
            request.getFixedBonus(),
            request.getProgressiveConfig(),
            request.getImprovementCoefficient(),
            request.getDescription(),
            request.getSortOrder()
        );
        return Result.success(BonusItemResponse.fromDomain(item));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a bonus item")
    @PreAuthorize("hasAuthority('inspection:template:edit')")
    public Result<Void> delete(@PathVariable Long id) {
        bonusItemService.deleteBonusItem(id);
        return Result.success(null);
    }
}
