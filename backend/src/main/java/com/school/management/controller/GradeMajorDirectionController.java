package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.entity.GradeMajorDirection;
import com.school.management.service.GradeMajorDirectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学年专业方向关联管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/grade-major-directions")
@RequiredArgsConstructor
public class GradeMajorDirectionController {

    private final GradeMajorDirectionService gradeMajorDirectionService;

    /**
     * 分页查询学年专业方向关联列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('grade:direction:list')")
    public Result<IPage<GradeMajorDirection>> getGradeMajorDirectionPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            GradeMajorDirection params) {
        log.info("分页查询学年专业方向关联列表: pageNum={}, pageSize={}, params={}", pageNum, pageSize, params);
        IPage<GradeMajorDirection> page = gradeMajorDirectionService.getGradeMajorDirectionPage(pageNum, pageSize, params);
        return Result.success(page);
    }

    /**
     * 根据学年查询专业方向列表
     */
    @GetMapping("/year/{academicYear}")
    @PreAuthorize("hasAuthority('grade:direction:list')")
    public Result<List<GradeMajorDirection>> getByAcademicYear(@PathVariable Integer academicYear) {
        log.info("根据学年查询专业方向列表: academicYear={}", academicYear);
        List<GradeMajorDirection> list = gradeMajorDirectionService.getByAcademicYear(academicYear);
        return Result.success(list);
    }

    /**
     * 根据专业方向ID查询关联的学年列表
     */
    @GetMapping("/direction/{directionId}")
    @PreAuthorize("hasAuthority('grade:direction:list')")
    public Result<List<GradeMajorDirection>> getByMajorDirectionId(@PathVariable Long directionId) {
        log.info("根据专业方向ID查询关联的学年列表: directionId={}", directionId);
        List<GradeMajorDirection> list = gradeMajorDirectionService.getByMajorDirectionId(directionId);
        return Result.success(list);
    }

    /**
     * 根据ID查询详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('grade:direction:info')")
    public Result<GradeMajorDirection> getDetailById(@PathVariable Long id) {
        log.info("查询学年专业方向详情: id={}", id);
        GradeMajorDirection detail = gradeMajorDirectionService.getDetailById(id);
        return Result.success(detail);
    }

    /**
     * 根据学年和专业方向ID查询
     */
    @GetMapping("/year/{academicYear}/direction/{directionId}")
    @PreAuthorize("hasAuthority('grade:direction:info')")
    public Result<GradeMajorDirection> getByYearAndDirection(
            @PathVariable Integer academicYear,
            @PathVariable Long directionId) {
        log.info("根据学年和专业方向ID查询: academicYear={}, directionId={}", academicYear, directionId);
        GradeMajorDirection result = gradeMajorDirectionService.getByYearAndDirection(academicYear, directionId);
        return Result.success(result);
    }

    /**
     * 为学年添加专业方向
     */
    @PostMapping
    @PreAuthorize("hasAuthority('grade:direction:add')")
    public Result<GradeMajorDirection> addDirectionToYear(@RequestBody GradeMajorDirection gradeMajorDirection) {
        log.info("为学年添加专业方向: {}", gradeMajorDirection);
        GradeMajorDirection created = gradeMajorDirectionService.addDirectionToYear(gradeMajorDirection);
        return Result.success(created);
    }

    /**
     * 批量为学年添加专业方向
     */
    @PostMapping("/year/{academicYear}/batch")
    @PreAuthorize("hasAuthority('grade:direction:add')")
    public Result<Void> batchAddDirectionsToYear(
            @PathVariable Integer academicYear,
            @RequestBody List<Long> directionIds) {
        log.info("批量为学年添加专业方向: academicYear={}, directionIds={}", academicYear, directionIds);
        gradeMajorDirectionService.batchAddDirectionsToYear(academicYear, directionIds);
        return Result.success();
    }

    /**
     * 更新学年专业方向配置
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('grade:direction:edit')")
    public Result<Void> updateGradeMajorDirection(
            @PathVariable Long id,
            @RequestBody GradeMajorDirection gradeMajorDirection) {
        log.info("更新学年专业方向配置: id={}, gradeMajorDirection={}", id, gradeMajorDirection);
        gradeMajorDirectionService.updateGradeMajorDirection(id, gradeMajorDirection);
        return Result.success();
    }

    /**
     * 删除学年专业方向关联
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('grade:direction:delete')")
    public Result<Void> deleteGradeMajorDirection(@PathVariable Long id) {
        log.info("删除学年专业方向关联: id={}", id);
        gradeMajorDirectionService.deleteGradeMajorDirection(id);
        return Result.success();
    }

    /**
     * 批量删除学年专业方向关联
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('grade:direction:delete')")
    public Result<Void> batchDeleteGradeMajorDirections(@RequestBody List<Long> ids) {
        log.info("批量删除学年专业方向关联: ids={}", ids);
        gradeMajorDirectionService.batchDeleteGradeMajorDirections(ids);
        return Result.success();
    }
}
