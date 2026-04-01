package com.school.management.interfaces.rest.academic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.application.academic.CourseApplicationService;
import com.school.management.application.academic.command.CreateCourseCommand;
import com.school.management.application.academic.command.UpdateCourseCommand;
import com.school.management.application.academic.query.CourseDTO;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程管理 REST 控制器
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management API")
@RestController
@RequestMapping("/academic/courses")
public class CourseController {

    private final CourseApplicationService courseService;

    @Operation(summary = "Get course list (paginated)")
    @GetMapping
    @CasbinAccess(resource = "teaching:course", action = "view")
    public Result<Page<CourseDTO>> listCourses(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer courseCategory,
            @RequestParam(required = false) Integer courseType,
            @RequestParam(required = false) Integer status) {
        return Result.success(courseService.getCourseList(keyword, courseCategory, courseType, status, pageNum, pageSize));
    }

    @Operation(summary = "Get all courses (for dropdowns)")
    @GetMapping("/all")
    @CasbinAccess(resource = "teaching:course", action = "view")
    public Result<List<CourseDTO>> listAllCourses() {
        return Result.success(courseService.getAllCourses());
    }

    @Operation(summary = "Get course by ID")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "teaching:course", action = "view")
    public Result<CourseDTO> getCourse(@PathVariable Long id) {
        return Result.success(courseService.getCourse(id));
    }

    @Operation(summary = "Get course by code")
    @GetMapping("/code/{code}")
    @CasbinAccess(resource = "teaching:course", action = "view")
    public Result<CourseDTO> getCourseByCode(@PathVariable String code) {
        return Result.success(courseService.getCourseByCode(code));
    }

    @Operation(summary = "Create course")
    @PostMapping
    @CasbinAccess(resource = "teaching:course", action = "edit")
    public Result<CourseDTO> createCourse(@RequestBody CreateCourseCommand command) {
        command.setCreatedBy(SecurityUtils.requireCurrentUserId());
        return Result.success(courseService.createCourse(command));
    }

    @Operation(summary = "Update course")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "teaching:course", action = "edit")
    public Result<CourseDTO> updateCourse(@PathVariable Long id,
                                           @RequestBody UpdateCourseCommand command) {
        command.setUpdatedBy(SecurityUtils.requireCurrentUserId());
        return Result.success(courseService.updateCourse(id, command));
    }

    @Operation(summary = "Delete course")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "teaching:course", action = "edit")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.success();
    }

    @Operation(summary = "Update course status")
    @PatchMapping("/{id}/status")
    @CasbinAccess(resource = "teaching:course", action = "edit")
    public Result<Void> updateCourseStatus(@PathVariable Long id,
                                            @RequestBody Map<String, Object> data) {
        Integer status = ((Number) data.get("status")).intValue();
        courseService.updateCourseStatus(id, status);
        return Result.success();
    }
}
