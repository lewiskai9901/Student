package com.school.management.infrastructure.extension.plugins.education.interfaces.rest.teacher;

import com.school.management.infrastructure.extension.plugins.education.application.teacher.TeacherProfileApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Teacher Profile REST Controller
 *
 * Manages teacher academic profiles (title, qualifications, course capabilities).
 * Data access is delegated to {@link TeacherProfileApplicationService}.
 */
@Slf4j
@RestController
@RequestMapping("/teacher-profiles")
@RequiredArgsConstructor
@Tag(name = "TeacherProfiles", description = "Teacher profile management API")
public class TeacherProfileController {

    private final TeacherProfileApplicationService teacherProfileService;

    // ==================== List (paginated) ====================

    @Operation(summary = "Get teacher profile list (paginated)")
    @GetMapping
    @CasbinAccess(resource = "teacher:profile", action = "view")
    public Result<Map<String, Object>> listProfiles(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(
            teacherProfileService.listProfiles(pageNum, pageSize, orgUnitId, title, status, keyword));
    }

    // ==================== Get by ID ====================

    @Operation(summary = "Get teacher profile by ID")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "teacher:profile", action = "view")
    public Result<Map<String, Object>> getProfile(@PathVariable Long id) {
        return Result.success(teacherProfileService.getProfile(id));
    }

    // ==================== Get by User ID ====================

    @Operation(summary = "Get teacher profile by user ID")
    @GetMapping("/by-user/{userId}")
    @CasbinAccess(resource = "teacher:profile", action = "view")
    public Result<Map<String, Object>> getByUserId(@PathVariable Long userId) {
        return Result.success(teacherProfileService.getByUserId(userId));
    }

    // ==================== Create ====================

    @Operation(summary = "Create teacher profile")
    @PostMapping
    @CasbinAccess(resource = "teacher:profile", action = "edit")
    public Result<Long> createProfile(@RequestBody Map<String, Object> data) {
        Long userId = toLong(data.get("userId"));
        if (userId == null) {
            return Result.error("userId is required");
        }
        Long id = teacherProfileService.createProfile(userId, data);
        if (id == null) {
            return Result.error("该用户已有教师档案");
        }
        return Result.success(id);
    }

    // ==================== Update ====================

    @Operation(summary = "Update teacher profile")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "teacher:profile", action = "edit")
    public Result<Void> updateProfile(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        teacherProfileService.updateProfile(id, data);
        return Result.success();
    }

    // ==================== Delete (soft) ====================

    @Operation(summary = "Delete teacher profile")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "teacher:profile", action = "edit")
    public Result<Void> deleteProfile(@PathVariable Long id) {
        teacherProfileService.deleteProfile(id);
        return Result.success();
    }

    // ==================== Course Qualifications ====================

    @Operation(summary = "Get courses for a teacher")
    @GetMapping("/{id}/courses")
    @CasbinAccess(resource = "teacher:profile", action = "view")
    public Result<List<Map<String, Object>>> getCourses(@PathVariable Long id) {
        return Result.success(teacherProfileService.getCourses(id));
    }

    @Operation(summary = "Add course qualification to teacher")
    @PostMapping("/{id}/courses")
    @CasbinAccess(resource = "teacher:profile", action = "edit")
    public Result<Long> addCourse(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long courseId = toLong(data.get("courseId"));
        if (courseId == null) {
            return Result.error("courseId is required");
        }
        Long newId = teacherProfileService.addCourse(id, courseId, data);
        if (newId == null) {
            return Result.error("该教师已添加此课程资质");
        }
        return Result.success(newId);
    }

    @Operation(summary = "Remove course qualification from teacher")
    @DeleteMapping("/{id}/courses/{courseId}")
    @CasbinAccess(resource = "teacher:profile", action = "edit")
    public Result<Void> removeCourse(@PathVariable Long id, @PathVariable Long courseId) {
        teacherProfileService.removeCourse(id, courseId);
        return Result.success();
    }

    // ==================== Query: available teachers for a course ====================

    @Operation(summary = "Get teachers qualified for a specific course")
    @GetMapping("/available")
    @CasbinAccess(resource = "teacher:profile", action = "view")
    public Result<List<Map<String, Object>>> getAvailableForCourse(
            @RequestParam Long courseId) {
        return Result.success(teacherProfileService.getAvailableForCourse(courseId));
    }

    // ==================== Helpers ====================

    /** Parses an id value from the JSON request body (request binding helper). */
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
