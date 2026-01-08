package com.school.management.controller.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.entity.evaluation.Course;
import com.school.management.service.evaluation.CourseService;
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
 * 课程管理控制器
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/courses")
@RequiredArgsConstructor
@Tag(name = "课程管理", description = "课程管理相关接口")
public class CourseController {

    private final CourseService courseService;

    /**
     * 分页查询课程
     */
    @GetMapping
    @Operation(summary = "分页查询课程")
    @PreAuthorize("hasAuthority('evaluation:course:list')")
    public Result<PageResult<Map<String, Object>>> pageCourses(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "课程编码") @RequestParam(required = false) String courseCode,
            @Parameter(description = "课程名称") @RequestParam(required = false) String courseName,
            @Parameter(description = "课程类型") @RequestParam(required = false) String courseType,
            @Parameter(description = "学期ID") @RequestParam(required = false) Long semesterId,
            @Parameter(description = "系部ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        log.info("分页查询课程: pageNum={}, pageSize={}", pageNum, pageSize);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        Map<String, Object> query = new HashMap<>();
        query.put("courseCode", courseCode);
        query.put("courseName", courseName);
        query.put("courseType", courseType);
        query.put("semesterId", semesterId);
        query.put("departmentId", departmentId);
        query.put("status", status);

        Page<Map<String, Object>> result = courseService.pageCourses(page, query);
        return Result.success(PageResult.from(result));
    }

    /**
     * 获取课程详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取课程详情")
    @PreAuthorize("hasAuthority('evaluation:course:list')")
    public Result<Map<String, Object>> getCourseDetail(
            @Parameter(description = "课程ID") @PathVariable Long id) {
        log.info("获取课程详情: id={}", id);
        Map<String, Object> detail = courseService.getCourseDetail(id);
        return Result.success(detail);
    }

    /**
     * 创建课程
     */
    @PostMapping
    @Operation(summary = "创建课程")
    @PreAuthorize("hasAuthority('evaluation:course:create')")
    public Result<Long> createCourse(@RequestBody Course course) {
        log.info("创建课程: code={}, name={}", course.getCourseCode(), course.getCourseName());
        Long id = courseService.createCourse(course);
        return Result.success(id);
    }

    /**
     * 更新课程
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新课程")
    @PreAuthorize("hasAuthority('evaluation:course:update')")
    public Result<Void> updateCourse(
            @Parameter(description = "课程ID") @PathVariable Long id,
            @RequestBody Course course) {
        log.info("更新课程: id={}", id);
        course.setId(id);
        courseService.updateCourse(course);
        return Result.success();
    }

    /**
     * 删除课程
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除课程")
    @PreAuthorize("hasAuthority('evaluation:course:delete')")
    public Result<Void> deleteCourse(
            @Parameter(description = "课程ID") @PathVariable Long id) {
        log.info("删除课程: id={}", id);
        courseService.deleteCourse(id);
        return Result.success();
    }

    /**
     * 根据学期获取课程列表
     */
    @GetMapping("/semester/{semesterId}")
    @Operation(summary = "根据学期获取课程列表")
    @PreAuthorize("hasAuthority('evaluation:course:list')")
    public Result<List<Course>> getBySemester(
            @Parameter(description = "学期ID") @PathVariable Long semesterId) {
        log.info("根据学期获取课程列表: semesterId={}", semesterId);
        List<Course> list = courseService.getBySemesterId(semesterId);
        return Result.success(list);
    }

    /**
     * 根据班级获取课程列表
     */
    @GetMapping("/class/{classId}")
    @Operation(summary = "根据班级获取课程列表")
    @PreAuthorize("hasAuthority('evaluation:course:list')")
    public Result<List<Map<String, Object>>> getByClass(
            @Parameter(description = "班级ID") @PathVariable Long classId,
            @Parameter(description = "学期ID") @RequestParam(required = false) Long semesterId) {
        log.info("根据班级获取课程列表: classId={}", classId);
        List<Map<String, Object>> list = courseService.getByClassId(classId, semesterId);
        return Result.success(list);
    }

    /**
     * 获取课程类型列表
     */
    @GetMapping("/types")
    @Operation(summary = "获取课程类型列表")
    public Result<List<Map<String, Object>>> getCourseTypes() {
        List<Map<String, Object>> types = courseService.getCourseTypes();
        return Result.success(types);
    }

    /**
     * 导入课程数据
     */
    @PostMapping("/import")
    @Operation(summary = "导入课程数据")
    @PreAuthorize("hasAuthority('evaluation:course:import')")
    public Result<Integer> importCourses(
            @RequestBody List<Course> courses,
            @RequestParam Long semesterId) {
        log.info("导入课程数据: count={}, semesterId={}", courses.size(), semesterId);
        int count = courseService.importCourses(courses, semesterId);
        return Result.success(count);
    }
}
