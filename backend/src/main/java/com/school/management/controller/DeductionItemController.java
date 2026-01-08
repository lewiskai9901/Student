package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.DeductionItemCreateRequest;
import com.school.management.dto.DeductionItemUpdateRequest;
import com.school.management.entity.DeductionItem;
import com.school.management.service.DeductionItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 扣分项控制器
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/deduction-items")
@RequiredArgsConstructor
public class DeductionItemController {

    private final DeductionItemService deductionItemService;

    /**
     * 创建扣分项
     */
    @PostMapping
    @PreAuthorize("hasAuthority('quantification:config:add')")
    public Result<Long> createDeductionItem(@RequestBody DeductionItemCreateRequest request) {
        Long id = deductionItemService.createDeductionItem(request);
        return Result.success(id);
    }

    /**
     * 更新扣分项
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:config:edit')")
    public Result<Void> updateDeductionItem(@PathVariable Long id, @RequestBody DeductionItemUpdateRequest request) {
        request.setId(id);
        deductionItemService.updateDeductionItem(request);
        return Result.success();
    }

    /**
     * 删除扣分项
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:config:delete')")
    public Result<Void> deleteDeductionItem(@PathVariable Long id) {
        deductionItemService.deleteDeductionItem(id);
        return Result.success();
    }

    /**
     * 批量删除扣分项
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('quantification:config:delete')")
    public Result<Void> batchDeleteDeductionItems(@RequestBody List<Long> ids) {
        deductionItemService.deleteDeductionItems(ids);
        return Result.success();
    }

    /**
     * 获取扣分项详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<DeductionItem> getDeductionItem(@PathVariable Long id) {
        DeductionItem deductionItem = deductionItemService.getDeductionItemById(id);
        return Result.success(deductionItem);
    }

    /**
     * 根据类型ID获取扣分项列表
     */
    @GetMapping("/type/{typeId}")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<List<DeductionItem>> getDeductionItemsByTypeId(@PathVariable Long typeId) {
        List<DeductionItem> items = deductionItemService.getDeductionItemsByTypeId(typeId);
        return Result.success(items);
    }

    /**
     * 根据类型ID获取启用的扣分项列表
     */
    @GetMapping("/type/{typeId}/enabled")
    public Result<List<DeductionItem>> getEnabledDeductionItemsByTypeId(@PathVariable Long typeId) {
        List<DeductionItem> items = deductionItemService.getEnabledDeductionItemsByTypeId(typeId);
        return Result.success(items);
    }

    /**
     * 分页查询扣分项
     */
    @GetMapping
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<IPage<DeductionItem>> getDeductionItemPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long typeId) {
        IPage<DeductionItem> page = deductionItemService.getDeductionItemPage(pageNum, pageSize, typeId);
        return Result.success(page);
    }

    /**
     * 更新扣分项状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('quantification:config:edit')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        deductionItemService.updateStatus(id, status);
        return Result.success();
    }
}
