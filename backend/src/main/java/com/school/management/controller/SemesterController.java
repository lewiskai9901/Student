package com.school.management.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.entity.Semester;
import com.school.management.service.SemesterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 学期管理控制器
 *
 * @author system
 * @since 2.0.0
 */
@Tag(name = "学期管理")
@RestController
@RequestMapping("/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @Operation(summary = "分页查询学期列表")
    @GetMapping
    @PreAuthorize("hasAuthority('system:semester:list')")
    public Result<IPage<Semester>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String semesterName
    ) {
        Page<Semester> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Semester> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);

        if (semesterName != null && !semesterName.isEmpty()) {
            wrapper.like("semester_name", semesterName);
        }

        wrapper.orderByDesc("start_date");

        IPage<Semester> result = semesterService.page(page, wrapper);
        return Result.success(result);
    }

    @Operation(summary = "获取当前学期")
    @GetMapping("/current")
    public Result<Semester> getCurrent() {
        Semester semester = semesterService.getCurrentSemester();
        return Result.success(semester);
    }

    @Operation(summary = "根据ID查询学期")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:query')")
    public Result<Semester> getById(@PathVariable Long id) {
        Semester semester = semesterService.getById(id);
        return Result.success(semester);
    }

    @Operation(summary = "创建学期")
    @PostMapping
    @PreAuthorize("hasAuthority('system:semester:add')")
    public Result<Void> create(@RequestBody Semester semester) {
        // 生成学期编码
        String code = semesterService.generateSemesterCode(
            semester.getStartYear(),
            semester.getSemesterType()
        );
        semester.setSemesterCode(code);

        boolean success = semesterService.save(semester);
        return success ? Result.success() : Result.error("创建失败");
    }

    @Operation(summary = "更新学期")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Semester semester) {
        semester.setId(id);
        boolean success = semesterService.updateById(semester);
        return success ? Result.success() : Result.error("更新失败");
    }

    @Operation(summary = "删除学期")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = semesterService.removeById(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    @Operation(summary = "设置当前学期")
    @PutMapping("/{id}/set-current")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    public Result<Void> setCurrentSemester(@PathVariable Long id) {
        boolean success = semesterService.setCurrentSemester(id);
        return success ? Result.success() : Result.error("设置失败");
    }
}
