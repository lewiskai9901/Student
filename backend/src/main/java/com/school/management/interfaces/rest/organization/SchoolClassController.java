package com.school.management.interfaces.rest.organization;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.domain.organization.model.ClassStatus;
import com.school.management.domain.organization.model.SchoolClass;
import com.school.management.domain.organization.model.TeacherAssignment;
import com.school.management.domain.organization.repository.SchoolClassRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级管理 REST API (V2 - DDD架构)
 */
@Slf4j
@RestController
@RequestMapping("/v2/organization/classes")
@RequiredArgsConstructor
@Tag(name = "班级管理V2", description = "DDD架构的班级管理接口")
public class SchoolClassController {

    private final SchoolClassRepository schoolClassRepository;

    @GetMapping
    @Operation(summary = "获取班级列表")
    public Result<IPage<SchoolClassResponse>> getClasses(
            @Parameter(description = "组织单元ID") @RequestParam(required = false) Long orgUnitId,
            @Parameter(description = "入学年份") @RequestParam(required = false) Integer enrollmentYear,
            @Parameter(description = "班级状态") @RequestParam(required = false) ClassStatus status,
            @Parameter(description = "专业方向ID") @RequestParam(required = false) Long majorDirectionId,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {

        // 根据条件查询
        List<SchoolClass> classes;
        if (orgUnitId != null && enrollmentYear != null) {
            classes = schoolClassRepository.findByOrgUnitIdAndEnrollmentYear(orgUnitId, enrollmentYear);
        } else if (orgUnitId != null) {
            classes = schoolClassRepository.findByOrgUnitId(orgUnitId);
        } else if (enrollmentYear != null) {
            classes = schoolClassRepository.findByEnrollmentYear(enrollmentYear);
        } else if (status != null) {
            classes = schoolClassRepository.findByStatus(status);
        } else if (majorDirectionId != null) {
            classes = schoolClassRepository.findByMajorDirectionId(majorDirectionId);
        } else {
            classes = schoolClassRepository.findByStatus(ClassStatus.ACTIVE);
        }

        // 关键词过滤
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            classes = classes.stream()
                    .filter(c -> c.getClassName().toLowerCase().contains(kw)
                            || c.getClassCode().toLowerCase().contains(kw))
                    .collect(Collectors.toList());
        }

        // 手动分页
        int total = classes.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<SchoolClassResponse> pageData = classes.subList(fromIndex, toIndex).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Page<SchoolClassResponse> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pageData);

        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取班级详情")
    public Result<SchoolClassResponse> getClass(@PathVariable Long id) {
        return schoolClassRepository.findById(id)
                .map(this::toResponse)
                .map(Result::success)
                .orElse(Result.error("班级不存在"));
    }

    @GetMapping("/code/{classCode}")
    @Operation(summary = "根据编码获取班级")
    public Result<SchoolClassResponse> getClassByCode(@PathVariable String classCode) {
        return schoolClassRepository.findByClassCode(classCode)
                .map(this::toResponse)
                .map(Result::success)
                .orElse(Result.error("班级不存在"));
    }

    @PostMapping
    @Operation(summary = "创建班级")
    public Result<SchoolClassResponse> createClass(@RequestBody CreateClassRequest request) {
        // 检查编码是否存在
        if (schoolClassRepository.existsByClassCode(request.getClassCode())) {
            return Result.error("班级编码已存在");
        }

        Long currentUserId = getCurrentUserId();

        SchoolClass schoolClass = SchoolClass.create(
                request.getClassCode(),
                request.getClassName(),
                request.getOrgUnitId(),
                request.getEnrollmentYear(),
                request.getMajorDirectionId(),
                currentUserId
        );

        // 设置可选属性
        if (request.getShortName() != null) {
            schoolClass.updateInfo(null, request.getShortName(), request.getStandardSize(), null, currentUserId);
        }

        SchoolClass saved = schoolClassRepository.save(schoolClass);
        return Result.success(toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新班级")
    public Result<SchoolClassResponse> updateClass(@PathVariable Long id, @RequestBody UpdateClassRequest request) {
        return schoolClassRepository.findById(id)
                .map(schoolClass -> {
                    schoolClass.updateInfo(
                            request.getClassName(),
                            request.getShortName(),
                            request.getStandardSize(),
                            request.getSortOrder(),
                            getCurrentUserId()
                    );
                    return schoolClassRepository.save(schoolClass);
                })
                .map(this::toResponse)
                .map(Result::success)
                .orElse(Result.error("班级不存在"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除班级")
    public Result<Void> deleteClass(@PathVariable Long id) {
        return schoolClassRepository.findById(id)
                .map(schoolClass -> {
                    schoolClassRepository.deleteById(schoolClass.getId());
                    return Result.<Void>success(null);
                })
                .orElse(Result.error("班级不存在"));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活班级")
    public Result<Void> activateClass(@PathVariable Long id) {
        return schoolClassRepository.findById(id)
                .map(schoolClass -> {
                    schoolClass.activate(getCurrentUserId());
                    schoolClassRepository.save(schoolClass);
                    return Result.<Void>success(null);
                })
                .orElse(Result.error("班级不存在"));
    }

    @PostMapping("/{id}/graduate")
    @Operation(summary = "班级毕业")
    public Result<Void> graduateClass(@PathVariable Long id) {
        return schoolClassRepository.findById(id)
                .map(schoolClass -> {
                    schoolClass.graduate(getCurrentUserId());
                    schoolClassRepository.save(schoolClass);
                    return Result.<Void>success(null);
                })
                .orElse(Result.error("班级不存在"));
    }

    @PostMapping("/{id}/dissolve")
    @Operation(summary = "撤销班级")
    public Result<Void> dissolveClass(@PathVariable Long id) {
        return schoolClassRepository.findById(id)
                .map(schoolClass -> {
                    schoolClass.dissolve(getCurrentUserId());
                    schoolClassRepository.save(schoolClass);
                    return Result.<Void>success(null);
                })
                .orElse(Result.error("班级不存在"));
    }

    @PostMapping("/{id}/head-teacher")
    @Operation(summary = "分配班主任")
    public Result<Void> assignHeadTeacher(@PathVariable Long id, @RequestBody AssignTeacherRequest request) {
        return schoolClassRepository.findById(id)
                .map(schoolClass -> {
                    schoolClass.assignHeadTeacher(request.getTeacherId(), request.getTeacherName(), getCurrentUserId());
                    schoolClassRepository.save(schoolClass);
                    return Result.<Void>success(null);
                })
                .orElse(Result.error("班级不存在"));
    }

    @PostMapping("/{id}/deputy-head-teacher")
    @Operation(summary = "分配副班主任")
    public Result<Void> assignDeputyHeadTeacher(@PathVariable Long id, @RequestBody AssignTeacherRequest request) {
        return schoolClassRepository.findById(id)
                .map(schoolClass -> {
                    schoolClass.assignDeputyHeadTeacher(request.getTeacherId(), request.getTeacherName(), getCurrentUserId());
                    schoolClassRepository.save(schoolClass);
                    return Result.<Void>success(null);
                })
                .orElse(Result.error("班级不存在"));
    }

    @GetMapping("/head-teacher/{teacherId}")
    @Operation(summary = "获取班主任管理的班级")
    public Result<List<SchoolClassResponse>> getClassesByHeadTeacher(@PathVariable Long teacherId) {
        List<SchoolClassResponse> classes = schoolClassRepository.findByHeadTeacherId(teacherId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return Result.success(classes);
    }

    @GetMapping("/graduating")
    @Operation(summary = "获取即将毕业的班级")
    public Result<List<SchoolClassResponse>> getGraduatingClasses(@RequestParam Integer year) {
        List<SchoolClassResponse> classes = schoolClassRepository.findGraduatingClasses(year).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return Result.success(classes);
    }

    @GetMapping("/check-code")
    @Operation(summary = "检查班级编码是否存在")
    public Result<Boolean> checkClassCodeExists(@RequestParam String classCode) {
        return Result.success(schoolClassRepository.existsByClassCode(classCode));
    }

    // ==================== 辅助方法 ====================

    private Long getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof com.school.management.security.CustomUserDetails) {
                return ((com.school.management.security.CustomUserDetails) principal).getId();
            }
        } catch (Exception e) {
            log.warn("Cannot get current user id", e);
        }
        return null;
    }

    private SchoolClassResponse toResponse(SchoolClass schoolClass) {
        SchoolClassResponse response = new SchoolClassResponse();
        response.setId(schoolClass.getId());
        response.setClassCode(schoolClass.getClassCode());
        response.setClassName(schoolClass.getClassName());
        response.setShortName(schoolClass.getShortName());
        response.setOrgUnitId(schoolClass.getOrgUnitId());
        response.setEnrollmentYear(schoolClass.getEnrollmentYear());
        response.setGradeLevel(schoolClass.getGradeLevel());
        response.setMajorDirectionId(schoolClass.getMajorDirectionId());
        response.setSchoolingYears(schoolClass.getSchoolingYears());
        response.setStandardSize(schoolClass.getStandardSize());
        response.setCurrentSize(schoolClass.getCurrentSize());
        response.setStatus(schoolClass.getStatus().name());
        response.setSortOrder(schoolClass.getSortOrder());
        response.setExpectedGraduationYear(schoolClass.getExpectedGraduationYear());
        response.setAvailableSlots(schoolClass.getAvailableSlots());
        response.setCreatedAt(schoolClass.getCreatedAt());
        response.setUpdatedAt(schoolClass.getUpdatedAt());

        // 教师信息
        schoolClass.getCurrentHeadTeacher().ifPresent(t -> {
            response.setHeadTeacherId(t.getTeacherId());
            response.setHeadTeacherName(t.getTeacherName());
        });

        response.setTeacherAssignments(schoolClass.getTeacherAssignments().stream()
                .map(this::toTeacherResponse)
                .collect(Collectors.toList()));

        return response;
    }

    private TeacherAssignmentResponse toTeacherResponse(TeacherAssignment assignment) {
        TeacherAssignmentResponse response = new TeacherAssignmentResponse();
        response.setTeacherId(assignment.getTeacherId());
        response.setTeacherName(assignment.getTeacherName());
        response.setRole(assignment.getRole().name());
        response.setStartDate(assignment.getStartDate());
        response.setEndDate(assignment.getEndDate());
        response.setCurrent(assignment.isCurrent());
        return response;
    }
}
