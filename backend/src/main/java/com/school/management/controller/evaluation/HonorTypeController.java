package com.school.management.controller.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.entity.evaluation.HonorType;
import com.school.management.service.evaluation.HonorTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 荣誉类型管理控制器
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/honor-types")
@RequiredArgsConstructor
@Tag(name = "荣誉类型管理", description = "荣誉类型字典管理相关接口")
public class HonorTypeController {

    private final HonorTypeService honorTypeService;

    /**
     * 分页查询荣誉类型
     */
    @GetMapping
    @Operation(summary = "分页查询荣誉类型")
    @PreAuthorize("hasAuthority('evaluation:honor:list')")
    public Result<PageResult<HonorType>> pageHonorTypes(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "类型编码") @RequestParam(required = false) String typeCode,
            @Parameter(description = "类型名称") @RequestParam(required = false) String typeName,
            @Parameter(description = "类别") @RequestParam(required = false) String category,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        log.info("分页查询荣誉类型: pageNum={}, pageSize={}", pageNum, pageSize);

        Page<HonorType> page = new Page<>(pageNum, pageSize);
        Map<String, Object> query = new HashMap<>();
        query.put("typeCode", typeCode);
        query.put("typeName", typeName);
        query.put("category", category);
        query.put("status", status);

        Page<HonorType> result = honorTypeService.pageHonorTypes(page, query);
        return Result.success(PageResult.from(result));
    }

    /**
     * 获取荣誉类型详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取荣誉类型详情")
    @PreAuthorize("hasAuthority('evaluation:honor:list')")
    public Result<Map<String, Object>> getHonorTypeDetail(
            @Parameter(description = "荣誉类型ID") @PathVariable Long id) {
        log.info("获取荣誉类型详情: id={}", id);
        Map<String, Object> detail = honorTypeService.getHonorTypeDetail(id);
        return Result.success(detail);
    }

    /**
     * 创建荣誉类型
     */
    @PostMapping
    @Operation(summary = "创建荣誉类型")
    @PreAuthorize("hasAuthority('evaluation:honor:create')")
    public Result<Long> createHonorType(@RequestBody HonorType honorType) {
        log.info("创建荣誉类型: code={}, name={}", honorType.getTypeCode(), honorType.getTypeName());
        Long id = honorTypeService.createHonorType(honorType);
        return Result.success(id);
    }

    /**
     * 更新荣誉类型
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新荣誉类型")
    @PreAuthorize("hasAuthority('evaluation:honor:update')")
    public Result<Void> updateHonorType(
            @Parameter(description = "荣誉类型ID") @PathVariable Long id,
            @RequestBody HonorType honorType) {
        log.info("更新荣誉类型: id={}", id);
        honorType.setId(id);
        honorTypeService.updateHonorType(honorType);
        return Result.success();
    }

    /**
     * 删除荣誉类型
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除荣誉类型")
    @PreAuthorize("hasAuthority('evaluation:honor:delete')")
    public Result<Void> deleteHonorType(
            @Parameter(description = "荣誉类型ID") @PathVariable Long id) {
        log.info("删除荣誉类型: id={}", id);
        honorTypeService.deleteHonorType(id);
        return Result.success();
    }

    /**
     * 根据类别获取荣誉类型列表
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "根据类别获取荣誉类型列表")
    @PreAuthorize("hasAuthority('evaluation:honor:list')")
    public Result<List<HonorType>> getByCategory(
            @Parameter(description = "荣誉类别") @PathVariable String category) {
        log.info("根据类别获取荣誉类型: category={}", category);
        List<HonorType> list = honorTypeService.getByCategory(category);
        return Result.success(list);
    }

    /**
     * 获取所有启用的荣誉类型
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的荣誉类型")
    @PreAuthorize("hasAuthority('evaluation:honor:list')")
    public Result<List<HonorType>> getAllEnabled() {
        log.info("获取所有启用的荣誉类型");
        List<HonorType> list = honorTypeService.getAllEnabled();
        return Result.success(list);
    }

    /**
     * 获取可用的荣誉类型（用于申报选择）
     */
    @GetMapping("/available")
    @Operation(summary = "获取可用的荣誉类型")
    public Result<List<Map<String, Object>>> getAvailableTypes() {
        log.info("获取可用的荣誉类型");
        List<Map<String, Object>> list = honorTypeService.getAvailableTypes();
        return Result.success(list);
    }

    /**
     * 检查编码是否存在
     */
    @GetMapping("/check-code")
    @Operation(summary = "检查编码是否存在")
    @PreAuthorize("hasAuthority('evaluation:honor:list')")
    public Result<Boolean> checkCodeExists(
            @Parameter(description = "类型编码") @RequestParam String code,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = honorTypeService.existsByCode(code, excludeId);
        return Result.success(exists);
    }
}
