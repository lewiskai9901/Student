package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.entity.Classroom;
import com.school.management.service.ClassroomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 教室管理控制器
 *
 * @author system
 * @since 1.0.0
 */
@Tag(name = "教室管理")
@RestController
@RequestMapping("/teaching/classrooms")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;

    @Operation(summary = "分页查询教室")
    @GetMapping
    @PreAuthorize("hasAuthority('teaching:classroom:list')")
    public Result<PageResult<Classroom>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "教学楼ID") @RequestParam(required = false) Long buildingId,
            @Parameter(description = "楼层") @RequestParam(required = false) Integer floor,
            @Parameter(description = "教室类型") @RequestParam(required = false) String classroomType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        Page<Classroom> page = new Page<>(pageNum, pageSize);
        IPage<Classroom> result = classroomService.page(page, buildingId, floor, classroomType, status);
        return Result.success(PageResult.from(result));
    }

    @Operation(summary = "获取教室详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:classroom:view')")
    public Result<Classroom> getById(@Parameter(description = "教室ID") @PathVariable Long id) {
        return Result.success(classroomService.getById(id));
    }

    @Operation(summary = "创建教室")
    @PostMapping
    @PreAuthorize("hasAuthority('teaching:classroom:add')")
    public Result<Classroom> create(@RequestBody Classroom classroom) {
        return Result.success(classroomService.create(classroom));
    }

    @Operation(summary = "更新教室")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:classroom:edit')")
    public Result<Classroom> update(
            @Parameter(description = "教室ID") @PathVariable Long id,
            @RequestBody Classroom classroom) {
        return Result.success(classroomService.update(id, classroom));
    }

    @Operation(summary = "删除教室")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:classroom:delete')")
    public Result<Void> delete(@Parameter(description = "教室ID") @PathVariable Long id) {
        classroomService.delete(id);
        return Result.success();
    }

    @Operation(summary = "关联班级到教室")
    @PostMapping("/{id}/assign-class")
    @PreAuthorize("hasAuthority('teaching:classroom:edit')")
    public Result<Classroom> assignClass(
            @Parameter(description = "教室ID") @PathVariable Long id,
            @Parameter(description = "班级ID") @RequestParam Long classId) {
        return Result.success(classroomService.assignClass(id, classId));
    }
}
