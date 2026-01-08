package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.entity.MajorDirection;
import com.school.management.service.MajorDirectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专业方向管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/major-directions")
@RequiredArgsConstructor
public class MajorDirectionController {

    private final MajorDirectionService majorDirectionService;

    /**
     * 分页查询专业方向列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('major:direction:list')")
    public Result<IPage<MajorDirection>> getDirectionPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            MajorDirection params) {
        log.info("分页查询专业方向列表: pageNum={}, pageSize={}, params={}", pageNum, pageSize, params);
        IPage<MajorDirection> page = majorDirectionService.getDirectionPage(pageNum, pageSize, params);
        return Result.success(page);
    }

    /**
     * 查询所有专业方向
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('major:direction:list')")
    public Result<List<MajorDirection>> getAllDirections() {
        log.info("查询所有专业方向");
        List<MajorDirection> directions = majorDirectionService.getAllDirections();
        return Result.success(directions);
    }

    /**
     * 根据专业ID查询专业方向列表
     */
    @GetMapping("/major/{majorId}")
    @PreAuthorize("hasAuthority('major:direction:list')")
    public Result<List<MajorDirection>> getDirectionsByMajorId(@PathVariable Long majorId) {
        log.info("根据专业ID查询专业方向列表: majorId={}", majorId);
        List<MajorDirection> directions = majorDirectionService.getDirectionsByMajorId(majorId);
        return Result.success(directions);
    }

    /**
     * 根据ID查询专业方向详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('major:direction:info')")
    public Result<MajorDirection> getDirectionById(@PathVariable Long id) {
        log.info("查询专业方向详情: id={}", id);
        MajorDirection direction = majorDirectionService.getDirectionById(id);
        return Result.success(direction);
    }

    /**
     * 创建专业方向
     */
    @PostMapping
    @PreAuthorize("hasAuthority('major:direction:add')")
    public Result<MajorDirection> createDirection(@RequestBody MajorDirection direction) {
        log.info("创建专业方向: {}", direction);
        MajorDirection created = majorDirectionService.createDirection(direction);
        return Result.success(created);
    }

    /**
     * 更新专业方向
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('major:direction:edit')")
    public Result<Void> updateDirection(@PathVariable Long id, @RequestBody MajorDirection direction) {
        log.info("更新专业方向: id={}, direction={}", id, direction);
        majorDirectionService.updateDirection(id, direction);
        return Result.success();
    }

    /**
     * 删除专业方向
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('major:direction:delete')")
    public Result<Void> deleteDirection(@PathVariable Long id) {
        log.info("删除专业方向: id={}", id);
        majorDirectionService.deleteDirection(id);
        return Result.success();
    }

    /**
     * 批量删除专业方向
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('major:direction:delete')")
    public Result<Void> batchDeleteDirections(@RequestBody List<Long> ids) {
        log.info("批量删除专业方向: ids={}", ids);
        majorDirectionService.batchDeleteDirections(ids);
        return Result.success();
    }
}
