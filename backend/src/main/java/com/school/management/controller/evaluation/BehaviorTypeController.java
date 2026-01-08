package com.school.management.controller.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.entity.evaluation.BehaviorType;
import com.school.management.service.evaluation.BehaviorTypeService;
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
 * 行为类型管理控制器
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/behavior-types")
@RequiredArgsConstructor
@Tag(name = "行为类型管理", description = "行为类型字典管理相关接口")
public class BehaviorTypeController {

    private final BehaviorTypeService behaviorTypeService;

    /**
     * 分页查询行为类型
     */
    @GetMapping
    @Operation(summary = "分页查询行为类型")
    @PreAuthorize("hasAuthority('evaluation:behavior:list')")
    public Result<PageResult<Map<String, Object>>> pageBehaviorTypes(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "行为编码") @RequestParam(required = false) String behaviorCode,
            @Parameter(description = "行为名称") @RequestParam(required = false) String behaviorName,
            @Parameter(description = "行为类别") @RequestParam(required = false) String behaviorCategory,
            @Parameter(description = "行为性质") @RequestParam(required = false) Integer behaviorNature,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        log.info("分页查询行为类型: pageNum={}, pageSize={}", pageNum, pageSize);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        Map<String, Object> query = new HashMap<>();
        query.put("behaviorCode", behaviorCode);
        query.put("behaviorName", behaviorName);
        query.put("behaviorCategory", behaviorCategory);
        query.put("behaviorNature", behaviorNature);
        query.put("status", status);

        Page<Map<String, Object>> result = behaviorTypeService.pageBehaviorTypes(page, query);
        return Result.success(PageResult.from(result));
    }

    /**
     * 获取行为类型详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取行为类型详情")
    @PreAuthorize("hasAuthority('evaluation:behavior:list')")
    public Result<Map<String, Object>> getBehaviorTypeDetail(
            @Parameter(description = "行为类型ID") @PathVariable Long id) {
        log.info("获取行为类型详情: id={}", id);
        Map<String, Object> detail = behaviorTypeService.getBehaviorTypeDetail(id);
        return Result.success(detail);
    }

    /**
     * 创建行为类型
     */
    @PostMapping
    @Operation(summary = "创建行为类型")
    @PreAuthorize("hasAuthority('evaluation:behavior:create')")
    public Result<Long> createBehaviorType(@RequestBody BehaviorType behaviorType) {
        log.info("创建行为类型: code={}, name={}", behaviorType.getBehaviorCode(), behaviorType.getBehaviorName());
        Long id = behaviorTypeService.createBehaviorType(behaviorType);
        return Result.success(id);
    }

    /**
     * 更新行为类型
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新行为类型")
    @PreAuthorize("hasAuthority('evaluation:behavior:update')")
    public Result<Void> updateBehaviorType(
            @Parameter(description = "行为类型ID") @PathVariable Long id,
            @RequestBody BehaviorType behaviorType) {
        log.info("更新行为类型: id={}", id);
        behaviorType.setId(id);
        behaviorTypeService.updateBehaviorType(behaviorType);
        return Result.success();
    }

    /**
     * 删除行为类型
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除行为类型")
    @PreAuthorize("hasAuthority('evaluation:behavior:delete')")
    public Result<Void> deleteBehaviorType(
            @Parameter(description = "行为类型ID") @PathVariable Long id) {
        log.info("删除行为类型: id={}", id);
        behaviorTypeService.deleteBehaviorType(id);
        return Result.success();
    }

    /**
     * 根据类别获取行为类型列表
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "根据类别获取行为类型列表")
    @PreAuthorize("hasAuthority('evaluation:behavior:list')")
    public Result<List<BehaviorType>> getByCategory(
            @Parameter(description = "行为类别") @PathVariable String category) {
        log.info("根据类别获取行为类型: category={}", category);
        List<BehaviorType> list = behaviorTypeService.getByCategory(category);
        return Result.success(list);
    }

    /**
     * 获取所有行为类别
     */
    @GetMapping("/categories")
    @Operation(summary = "获取所有行为类别")
    @PreAuthorize("hasAuthority('evaluation:behavior:list')")
    public Result<List<Map<String, Object>>> getAllCategories() {
        log.info("获取所有行为类别");
        List<Map<String, Object>> categories = behaviorTypeService.getAllCategories();
        return Result.success(categories);
    }

    /**
     * 检查编码是否存在
     */
    @GetMapping("/check-code")
    @Operation(summary = "检查编码是否存在")
    @PreAuthorize("hasAuthority('evaluation:behavior:list')")
    public Result<Boolean> checkCodeExists(
            @Parameter(description = "行为编码") @RequestParam String code,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = behaviorTypeService.isCodeExists(code, excludeId);
        return Result.success(exists);
    }
}
