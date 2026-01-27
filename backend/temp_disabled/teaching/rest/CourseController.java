package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.CourseApplicationService;
import com.school.management.application.teaching.command.CreateCourseCommand;
import com.school.management.application.teaching.command.UpdateCourseCommand;
import com.school.management.application.teaching.query.CourseDTO;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程管理控制器
 */
@Tag(name = "课程管理", description = "课程信息的增删改查")
@RestController
@RequestMapping("/api/v2/teaching/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseApplicationService courseService;

    @Operation(summary = "创建课程")
    @PostMapping
    @PreAuthorize("hasAuthority('teaching:course:create')")
    public Result<Long> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        CreateCourseCommand command = CreateCourseCommand.builder()
                .courseCode(request.getCourseCode())
                .courseName(request.getCourseName())
                .englishName(request.getEnglishName())
                .courseType(request.getCourseType())
                .courseNature(request.getCourseNature())
                .credits(request.getCredits())
                .totalHours(request.getTotalHours())
                .theoryHours(request.getTheoryHours())
                .labHours(request.getLabHours())
                .practiceHours(request.getPracticeHours())
                .weeklyHours(request.getWeeklyHours())
                .departmentId(request.getDepartmentId())
                .examType(request.getExamType())
                .gradeType(request.getGradeType())
                .prerequisites(request.getPrerequisites())
                .description(request.getDescription())
                .syllabus(request.getSyllabus())
                .operatorId(user.getId())
                .build();

        Long id = courseService.createCourse(command);
        return Result.success(id);
    }

    @Operation(summary = "更新课程")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:course:update')")
    public Result<Void> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        UpdateCourseCommand command = UpdateCourseCommand.builder()
                .id(id)
                .courseName(request.getCourseName())
                .englishName(request.getEnglishName())
                .courseType(request.getCourseType())
                .courseNature(request.getCourseNature())
                .credits(request.getCredits())
                .totalHours(request.getTotalHours())
                .theoryHours(request.getTheoryHours())
                .labHours(request.getLabHours())
                .practiceHours(request.getPracticeHours())
                .weeklyHours(request.getWeeklyHours())
                .departmentId(request.getDepartmentId())
                .examType(request.getExamType())
                .gradeType(request.getGradeType())
                .prerequisites(request.getPrerequisites())
                .description(request.getDescription())
                .syllabus(request.getSyllabus())
                .status(request.getStatus())
                .operatorId(user.getId())
                .build();

        courseService.updateCourse(command);
        return Result.success();
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:course:delete')")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.success();
    }

    @Operation(summary = "获取课程详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:course:view')")
    public Result<CourseDTO> getCourse(@PathVariable Long id) {
        return Result.success(courseService.getCourse(id));
    }

    @Operation(summary = "根据课程代码获取课程")
    @GetMapping("/code/{courseCode}")
    @PreAuthorize("hasAuthority('teaching:course:view')")
    public Result<CourseDTO> getCourseByCode(@PathVariable String courseCode) {
        return Result.success(courseService.getCourseByCode(courseCode));
    }

    @Operation(summary = "获取所有课程")
    @GetMapping
    @PreAuthorize("hasAuthority('teaching:course:view')")
    public Result<List<CourseDTO>> getAllCourses() {
        return Result.success(courseService.getAllCourses());
    }

    @Operation(summary = "根据部门获取课程")
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAuthority('teaching:course:view')")
    public Result<List<CourseDTO>> getCoursesByDepartment(@PathVariable Long departmentId) {
        return Result.success(courseService.getCoursesByDepartment(departmentId));
    }

    @Operation(summary = "根据课程类型获取课程")
    @GetMapping("/type/{courseType}")
    @PreAuthorize("hasAuthority('teaching:course:view')")
    public Result<List<CourseDTO>> getCoursesByType(@PathVariable Integer courseType) {
        return Result.success(courseService.getCoursesByType(courseType));
    }

    @Operation(summary = "搜索课程")
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('teaching:course:view')")
    public Result<List<CourseDTO>> searchCourses(@RequestParam String keyword) {
        return Result.success(courseService.searchCourses(keyword));
    }

    @Operation(summary = "分页查询课程")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('teaching:course:view')")
    public Result<PageResult<CourseDTO>> getCoursesPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer courseType,
            @RequestParam(required = false) Integer status) {

        List<CourseDTO> list = courseService.getCoursesPage(page, size, departmentId, courseType, status);
        long total = courseService.countCourses(departmentId, courseType, status);

        return Result.success(new PageResult<>(list, total, page, size));
    }
}
