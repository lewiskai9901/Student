package com.school.management.interfaces.rest.organization;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.organization.model.ClassStatus;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.model.SchoolClass;
import com.school.management.domain.organization.model.TeacherAssignment;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.organization.repository.SchoolClassRepository;
import com.school.management.infrastructure.persistence.organization.MajorDirectionPersistenceMapper;
import com.school.management.infrastructure.persistence.organization.MajorDirectionPO;
import com.school.management.infrastructure.persistence.organization.MajorPersistenceMapper;
import com.school.management.infrastructure.persistence.organization.MajorPO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 班级管理 REST API (V2 - DDD架构)
 */
@RestController("schoolClassController")
@RequestMapping("/organization/classes")
@Tag(name = "班级管理V2", description = "DDD架构的班级管理接口")
public class SchoolClassController {

    private static final Logger log = LoggerFactory.getLogger(SchoolClassController.class);

    private final SchoolClassRepository schoolClassRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MajorDirectionPersistenceMapper majorDirectionMapper;
    private final MajorPersistenceMapper majorMapper;

    public SchoolClassController(SchoolClassRepository schoolClassRepository,
                                 OrgUnitRepository orgUnitRepository,
                                 MajorDirectionPersistenceMapper majorDirectionMapper,
                                 MajorPersistenceMapper majorMapper) {
        this.schoolClassRepository = schoolClassRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.majorDirectionMapper = majorDirectionMapper;
        this.majorMapper = majorMapper;
    }

    @GetMapping
    @Operation(summary = "获取班级列表")
    @PreAuthorize("hasAuthority('class:view')")
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
        List<SchoolClass> pageClasses = classes.subList(fromIndex, toIndex);

        // 批量获取组织单元名称
        Map<Long, String> orgUnitNameMap = getOrgUnitNameMap(pageClasses);
        // 批量获取专业信息（专业名称和专业方向名称）
        Map<Long, MajorInfo> majorInfoMap = getMajorInfoMap(pageClasses);

        List<SchoolClassResponse> pageData = pageClasses.stream()
                .map(c -> toResponse(c, orgUnitNameMap.get(c.getOrgUnitId()), majorInfoMap.get(c.getMajorDirectionId())))
                .collect(Collectors.toList());

        Page<SchoolClassResponse> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pageData);

        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取班级详情")
    @PreAuthorize("hasAuthority('class:view')")
    public Result<SchoolClassResponse> getClass(@PathVariable Long id) {
        return schoolClassRepository.findById(id)
                .map(c -> toResponse(c, getOrgUnitName(c.getOrgUnitId()), getMajorInfo(c.getMajorDirectionId())))
                .map(Result::success)
                .orElse(Result.error("班级不存在"));
    }

    @GetMapping("/code/{classCode}")
    @Operation(summary = "根据编码获取班级")
    @PreAuthorize("hasAuthority('class:view')")
    public Result<SchoolClassResponse> getClassByCode(@PathVariable String classCode) {
        return schoolClassRepository.findByClassCode(classCode)
                .map(c -> toResponse(c, getOrgUnitName(c.getOrgUnitId()), getMajorInfo(c.getMajorDirectionId())))
                .map(Result::success)
                .orElse(Result.error("班级不存在"));
    }

    @PostMapping
    @Operation(summary = "创建班级")
    @PreAuthorize("hasAuthority('class:create')")
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

        // 如果请求状态为ACTIVE(1)，则激活班级
        if (request.getStatus() != null && request.getStatus() == 1) {
            schoolClass.activate(currentUserId);
        }

        SchoolClass saved = schoolClassRepository.save(schoolClass);
        return Result.success(toResponse(saved, getOrgUnitName(saved.getOrgUnitId()), getMajorInfo(saved.getMajorDirectionId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新班级")
    @PreAuthorize("hasAuthority('class:update')")
    public Result<SchoolClassResponse> updateClass(@PathVariable Long id, @RequestBody UpdateClassRequest request) {
        return schoolClassRepository.findById(id)
                .map(schoolClass -> {
                    Long currentUserId = getCurrentUserId();
                    // 更新基本信息
                    schoolClass.updateInfo(
                            request.getClassName(),
                            request.getShortName(),
                            request.getStandardSize(),
                            request.getSortOrder(),
                            currentUserId
                    );
                    // 更新组织关联信息（部门、年级、专业方向）
                    schoolClass.updateOrganization(
                            request.getOrgUnitId(),
                            request.getGradeId(),
                            request.getMajorDirectionId(),
                            currentUserId
                    );
                    // 更新状态
                    if (request.getStatus() != null) {
                        if ("ACTIVE".equals(request.getStatus()) && schoolClass.getStatus() == ClassStatus.PREPARING) {
                            schoolClass.activate(currentUserId);
                        }
                    }
                    return schoolClassRepository.save(schoolClass);
                })
                .map(c -> toResponse(c, getOrgUnitName(c.getOrgUnitId()), getMajorInfo(c.getMajorDirectionId())))
                .map(Result::success)
                .orElse(Result.error("班级不存在"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除班级")
    @PreAuthorize("hasAuthority('class:delete')")
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
    @PreAuthorize("hasAuthority('class:update')")
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
    @PreAuthorize("hasAuthority('class:update')")
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
    @PreAuthorize("hasAuthority('class:update')")
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
    @PreAuthorize("hasAuthority('class:update')")
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
    @PreAuthorize("hasAuthority('class:update')")
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
    @PreAuthorize("hasAuthority('class:view')")
    public Result<List<SchoolClassResponse>> getClassesByHeadTeacher(@PathVariable Long teacherId) {
        List<SchoolClass> classList = schoolClassRepository.findByHeadTeacherId(teacherId);
        Map<Long, String> orgUnitNameMap = getOrgUnitNameMap(classList);
        Map<Long, MajorInfo> majorInfoMap = getMajorInfoMap(classList);
        List<SchoolClassResponse> classes = classList.stream()
                .map(c -> toResponse(c, orgUnitNameMap.get(c.getOrgUnitId()), majorInfoMap.get(c.getMajorDirectionId())))
                .collect(Collectors.toList());
        return Result.success(classes);
    }

    @GetMapping("/graduating")
    @Operation(summary = "获取即将毕业的班级")
    @PreAuthorize("hasAuthority('class:view')")
    public Result<List<SchoolClassResponse>> getGraduatingClasses(@RequestParam Integer year) {
        List<SchoolClass> classList = schoolClassRepository.findGraduatingClasses(year);
        Map<Long, String> orgUnitNameMap = getOrgUnitNameMap(classList);
        Map<Long, MajorInfo> majorInfoMap = getMajorInfoMap(classList);
        List<SchoolClassResponse> classes = classList.stream()
                .map(c -> toResponse(c, orgUnitNameMap.get(c.getOrgUnitId()), majorInfoMap.get(c.getMajorDirectionId())))
                .collect(Collectors.toList());
        return Result.success(classes);
    }

    @GetMapping("/check-code")
    @Operation(summary = "检查班级编码是否存在")
    @PreAuthorize("hasAuthority('class:view')")
    public Result<Boolean> checkClassCodeExists(@RequestParam String classCode) {
        return Result.success(schoolClassRepository.existsByClassCode(classCode));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除班级")
    @PreAuthorize("hasAuthority('class:delete')")
    public Result<Integer> batchDeleteClasses(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的班级");
        }

        int deletedCount = 0;
        for (Long id : ids) {
            if (schoolClassRepository.findById(id).isPresent()) {
                schoolClassRepository.deleteById(id);
                deletedCount++;
            }
        }

        return Result.success(deletedCount);
    }

    @PostMapping("/{id}/teachers/{teacherId}/end")
    @Operation(summary = "结束教师任职")
    @PreAuthorize("hasAuthority('class:update')")
    public Result<Void> endTeacherAssignment(
            @PathVariable Long id,
            @PathVariable Long teacherId,
            @RequestParam String role) {
        return schoolClassRepository.findById(id)
                .map(schoolClass -> {
                    TeacherAssignment.TeacherRole teacherRole = TeacherAssignment.TeacherRole.valueOf(role);
                    schoolClass.endTeacherAssignment(teacherId, teacherRole);
                    schoolClassRepository.save(schoolClass);
                    return Result.<Void>success(null);
                })
                .orElse(Result.error("班级不存在"));
    }

    // ==================== 辅助方法 ====================

    private Long getCurrentUserId() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null) {
                return userId;
            }
        } catch (Exception e) {
            log.warn("Cannot get current user id", e);
        }
        // TODO: 应该抛出异常而不是返回默认值，待前端完善认证后修复
        return 1L;
    }

    private Map<Long, String> getOrgUnitNameMap(List<SchoolClass> classes) {
        Set<Long> orgUnitIds = classes.stream()
                .map(SchoolClass::getOrgUnitId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (orgUnitIds.isEmpty()) {
            return Map.of();
        }
        return orgUnitIds.stream()
                .map(id -> orgUnitRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(OrgUnit::getId, OrgUnit::getUnitName, (a, b) -> a));
    }

    private String getOrgUnitName(Long orgUnitId) {
        if (orgUnitId == null) {
            return null;
        }
        return orgUnitRepository.findById(orgUnitId)
                .map(OrgUnit::getUnitName)
                .orElse(null);
    }

    /**
     * 专业信息（包含专业名称、专业方向名称和正确的学制）
     */
    private record MajorInfo(String majorName, String directionName, Integer schoolingYears) {}

    /**
     * 计算正确的学制
     * 如果是分段注册，学制 = 第一阶段年数 + 第二阶段年数
     * 否则使用 years 字段
     */
    private Integer calculateSchoolingYears(MajorDirectionPO direction) {
        if (direction == null) {
            return null;
        }
        // 如果是分段注册，计算总学制
        if (direction.getIsSegmented() != null && direction.getIsSegmented() == 1) {
            int phase1 = direction.getPhase1Years() != null ? direction.getPhase1Years() : 0;
            int phase2 = direction.getPhase2Years() != null ? direction.getPhase2Years() : 0;
            return phase1 + phase2;
        }
        return direction.getYears();
    }

    private Map<Long, MajorInfo> getMajorInfoMap(List<SchoolClass> classes) {
        Set<Long> directionIds = classes.stream()
                .map(SchoolClass::getMajorDirectionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (directionIds.isEmpty()) {
            return Map.of();
        }

        // 获取专业方向信息
        List<MajorDirectionPO> directions = majorDirectionMapper.selectBatchIds(directionIds);

        // 获取关联的专业ID
        Set<Long> majorIds = directions.stream()
                .map(MajorDirectionPO::getMajorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // 获取专业名称映射
        Map<Long, String> majorNameMap = majorIds.isEmpty() ? Map.of() :
                majorMapper.selectBatchIds(majorIds).stream()
                        .collect(Collectors.toMap(MajorPO::getId, MajorPO::getMajorName, (a, b) -> a));

        // 构建专业方向ID -> MajorInfo 映射
        return directions.stream()
                .collect(Collectors.toMap(
                        MajorDirectionPO::getId,
                        d -> new MajorInfo(
                                majorNameMap.get(d.getMajorId()),
                                d.getDirectionName(),
                                calculateSchoolingYears(d)
                        ),
                        (a, b) -> a
                ));
    }

    private MajorInfo getMajorInfo(Long majorDirectionId) {
        if (majorDirectionId == null) {
            return new MajorInfo(null, null, null);
        }
        MajorDirectionPO direction = majorDirectionMapper.selectById(majorDirectionId);
        if (direction == null) {
            return new MajorInfo(null, null, null);
        }
        String majorName = null;
        if (direction.getMajorId() != null) {
            MajorPO major = majorMapper.selectById(direction.getMajorId());
            majorName = major != null ? major.getMajorName() : null;
        }
        return new MajorInfo(majorName, direction.getDirectionName(), calculateSchoolingYears(direction));
    }

    private SchoolClassResponse toResponse(SchoolClass schoolClass, String orgUnitName, MajorInfo majorInfo) {
        SchoolClassResponse response = new SchoolClassResponse();
        response.setId(schoolClass.getId());
        response.setClassCode(schoolClass.getClassCode());
        response.setClassName(schoolClass.getClassName());
        response.setShortName(schoolClass.getShortName());
        response.setOrgUnitId(schoolClass.getOrgUnitId());
        response.setOrgUnitName(orgUnitName);
        response.setEnrollmentYear(schoolClass.getEnrollmentYear());
        response.setGradeLevel(schoolClass.getGradeLevel());
        response.setGradeId(schoolClass.getGradeId());
        response.setMajorDirectionId(schoolClass.getMajorDirectionId());
        response.setMajorName(majorInfo != null ? majorInfo.majorName() : null);
        response.setMajorDirectionName(majorInfo != null ? majorInfo.directionName() : null);
        // 优先使用从专业方向计算出的学制（分段注册会计算总学制）
        response.setSchoolingYears(majorInfo != null && majorInfo.schoolingYears() != null
                ? majorInfo.schoolingYears()
                : schoolClass.getSchoolingYears());
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
