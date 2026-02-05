package com.school.management.interfaces.rest.scoring;

import com.school.management.application.scoring.InputTypeApplicationService;
import com.school.management.application.scoring.dto.*;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 打分方式控制器
 */
@Tag(name = "打分方式管理", description = "打分方式的CRUD操作")
@RestController
@RequestMapping("/scoring/input-types")
@RequiredArgsConstructor
public class InputTypeController {

    private final InputTypeApplicationService service;

    @Operation(summary = "获取所有打分方式（按分类分组）")
    @GetMapping("/grouped")
    public Result<Map<String, List<InputTypeDTO>>> getAllGrouped() {
        return Result.success(service.getAllGroupedByCategory());
    }

    @Operation(summary = "获取所有打分方式")
    @GetMapping
    public Result<List<InputTypeDTO>> getAll() {
        return Result.success(service.getAll());
    }

    @Operation(summary = "按分类获取打分方式")
    @GetMapping("/category/{category}")
    public Result<List<InputTypeDTO>> getByCategory(@PathVariable String category) {
        return Result.success(service.getByCategory(category));
    }

    @Operation(summary = "根据ID获取打分方式")
    @GetMapping("/{id}")
    public Result<InputTypeDTO> getById(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @Operation(summary = "根据代码获取打分方式")
    @GetMapping("/code/{code}")
    public Result<InputTypeDTO> getByCode(@PathVariable String code) {
        return Result.success(service.getByCode(code));
    }

    @Operation(summary = "创建自定义打分方式")
    @PostMapping
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<InputTypeDTO> create(@RequestBody CreateInputTypeCommand command) {
        return Result.success(service.create(command));
    }

    @Operation(summary = "更新打分方式")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<InputTypeDTO> update(@PathVariable Long id, @RequestBody UpdateInputTypeCommand command) {
        return Result.success(service.update(id, command));
    }

    @Operation(summary = "删除打分方式")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用打分方式")
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<Void> toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        service.toggleEnabled(id, enabled);
        return Result.success();
    }
}
