package com.school.management.infrastructure.persistence.student;

import com.school.management.domain.student.model.ClassStatus;
import com.school.management.domain.student.model.SchoolClass;
import com.school.management.domain.student.model.TeacherAssignment;
import com.school.management.domain.student.repository.SchoolClassRepository;
import com.school.management.infrastructure.persistence.organization.OrgUnitMapper;
import com.school.management.infrastructure.persistence.organization.OrgUnitPO;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import com.school.management.infrastructure.persistence.user.UserPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository implementation for SchoolClass.
 * Dual-table writes: each class has a shared Snowflake ID in both org_units and classes tables.
 * org_units provides tree identity; classes stores class-specific data.
 */
@Slf4j
@Repository
public class SchoolClassRepositoryImpl implements SchoolClassRepository {

    private final SchoolClassMapper classMapper;
    private final OrgUnitMapper orgUnitMapper;
    private final UserDomainMapper userDomainMapper;

    public SchoolClassRepositoryImpl(SchoolClassMapper classMapper,
                                     OrgUnitMapper orgUnitMapper,
                                     UserDomainMapper userDomainMapper) {
        this.classMapper = classMapper;
        this.orgUnitMapper = orgUnitMapper;
        this.userDomainMapper = userDomainMapper;
    }

    @Override
    public Optional<SchoolClass> findById(Long id) {
        SchoolClassPO po = classMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        if (schoolClass.getId() == null) {
            // INSERT: only write to org_units (classes VIEW auto-derives from it)
            OrgUnitPO orgPO = toOrgUnitPO(schoolClass);
            packClassAttributes(orgPO, schoolClass);
            orgUnitMapper.insert(orgPO);
            schoolClass.setId(orgPO.getId());
            updateTreePosition(orgPO.getId(), schoolClass.getOrgUnitId());
        } else {
            // UPDATE: only update org_units
            syncOrgUnit(schoolClass);
        }
        return schoolClass;
    }

    @Override
    public void delete(SchoolClass aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            orgUnitMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        orgUnitMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return classMapper.selectById(id) != null;
    }

    @Override
    public Optional<SchoolClass> findByClassCode(String classCode) {
        SchoolClassPO po = classMapper.findByClassCode(classCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<SchoolClass> findByOrgUnitId(Long orgUnitId) {
        return classMapper.findByOrgUnitId(orgUnitId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SchoolClass> findByGradeId(Long gradeId) {
        return classMapper.findByGradeId(gradeId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public int countByGradeId(Long gradeId) {
        return classMapper.countByGradeId(gradeId);
    }

    @Override
    public List<SchoolClass> findByEnrollmentYear(Integer enrollmentYear) {
        return classMapper.findByEnrollmentYear(enrollmentYear).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SchoolClass> findByStatus(ClassStatus status) {
        Integer dbStatus = toDbStatus(status);
        return classMapper.findByStatus(dbStatus).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SchoolClass> findByOrgUnitIdAndEnrollmentYear(Long orgUnitId, Integer enrollmentYear) {
        return classMapper.findByOrgUnitId(orgUnitId).stream()
            .filter(po -> enrollmentYear.equals(po.getEnrollmentYear()))
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SchoolClass> findByHeadTeacherId(Long teacherId) {
        return classMapper.findByTeacherId(teacherId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SchoolClass> findByTeacherId(Long teacherId) {
        return classMapper.findByTeacherId(teacherId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SchoolClass> findByMajorDirectionId(Long majorDirectionId) {
        return classMapper.findByMajorDirectionId(majorDirectionId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByClassCode(String classCode) {
        return classMapper.findByClassCode(classCode) != null;
    }

    @Override
    public int countByOrgUnitId(Long orgUnitId) {
        return classMapper.countByOrgUnitId(orgUnitId);
    }

    @Override
    public int countByStatus(ClassStatus status) {
        return classMapper.countByStatus(toDbStatus(status));
    }

    @Override
    public List<SchoolClass> findGraduatingClasses(Integer graduationYear) {
        return classMapper.findByGraduationYear(graduationYear).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    // ==================== Dual-table helpers ====================

    private OrgUnitPO toOrgUnitPO(SchoolClass schoolClass) {
        OrgUnitPO po = new OrgUnitPO();
        po.setUnitCode(schoolClass.getClassCode());
        po.setUnitName(schoolClass.getClassName());
        po.setUnitType("CLASS");
        po.setParentId(schoolClass.getOrgUnitId());
        po.setStatus(toOrgUnitStatus(schoolClass.getStatus()));
        po.setSortOrder(0);
        po.setCreatedAt(LocalDateTime.now());
        return po;
    }

    /**
     * Pack class-specific fields into org_units.attributes JSON.
     */
    private void packClassAttributes(OrgUnitPO po, SchoolClass schoolClass) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var attrs = new java.util.LinkedHashMap<String, Object>();

            // Merge existing attributes if present
            if (po.getAttributes() != null && !po.getAttributes().isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    var existing = mapper.readValue(po.getAttributes(), java.util.Map.class);
                    attrs.putAll(existing);
                } catch (Exception ignored) {}
            }

            if (schoolClass.getEnrollmentYear() != null) attrs.put("enrollmentYear", schoolClass.getEnrollmentYear());
            if (schoolClass.getGradeId() != null) attrs.put("gradeId", schoolClass.getGradeId());
            if (schoolClass.getGradeLevel() != null) attrs.put("gradeLevel", schoolClass.getGradeLevel());
            if (schoolClass.getMajorDirectionId() != null) attrs.put("majorDirectionId", schoolClass.getMajorDirectionId());
            attrs.put("studentCount", schoolClass.getCurrentSize());
            attrs.put("classType", 1);

            schoolClass.getCurrentHeadTeacher().ifPresent(t -> attrs.put("headTeacher", t.getTeacherId()));
            var deputies = schoolClass.getCurrentDeputyHeadTeachers();
            if (!deputies.isEmpty()) attrs.put("assistantTeacher", deputies.get(0).getTeacherId());

            po.setAttributes(mapper.writeValueAsString(attrs));
        } catch (Exception e) {
            log.warn("Failed to pack class attributes: {}", e.getMessage());
        }
    }

    private void syncOrgUnit(SchoolClass schoolClass) {
        OrgUnitPO existing = orgUnitMapper.selectById(schoolClass.getId());
        if (existing == null) {
            OrgUnitPO orgPO = toOrgUnitPO(schoolClass);
            orgPO.setId(schoolClass.getId());
            packClassAttributes(orgPO, schoolClass);
            orgUnitMapper.insert(orgPO);
            updateTreePosition(schoolClass.getId(), schoolClass.getOrgUnitId());
            return;
        }

        existing.setUnitName(schoolClass.getClassName());
        existing.setUnitCode(schoolClass.getClassCode());
        existing.setStatus(toOrgUnitStatus(schoolClass.getStatus()));
        existing.setUpdatedAt(LocalDateTime.now());
        packClassAttributes(existing, schoolClass);

        boolean parentChanged = (schoolClass.getOrgUnitId() == null && existing.getParentId() != null)
            || (schoolClass.getOrgUnitId() != null && !schoolClass.getOrgUnitId().equals(existing.getParentId()));
        if (parentChanged) existing.setParentId(schoolClass.getOrgUnitId());

        orgUnitMapper.updateById(existing);
        if (parentChanged) updateTreePosition(schoolClass.getId(), schoolClass.getOrgUnitId());
    }

    /**
     * Calculate and set tree_path and tree_level for a class node.
     */
    private void updateTreePosition(Long orgUnitId, Long parentOrgUnitId) {
        OrgUnitPO classPO = orgUnitMapper.selectById(orgUnitId);
        if (classPO == null) return;

        if (parentOrgUnitId != null) {
            OrgUnitPO parent = orgUnitMapper.selectById(parentOrgUnitId);
            if (parent != null) {
                String parentPath = parent.getTreePath() != null ? parent.getTreePath() : "/" + parent.getId() + "/";
                classPO.setTreePath(parentPath + orgUnitId + "/");
                classPO.setTreeLevel((parent.getTreeLevel() != null ? parent.getTreeLevel() : 0) + 1);
            } else {
                classPO.setTreePath("/" + orgUnitId + "/");
                classPO.setTreeLevel(1);
            }
        } else {
            classPO.setTreePath("/" + orgUnitId + "/");
            classPO.setTreeLevel(1);
        }

        orgUnitMapper.updateById(classPO);
    }

    // ==================== Mapping methods ====================

    private SchoolClassPO toPO(SchoolClass domain) {
        SchoolClassPO po = new SchoolClassPO();
        po.setId(domain.getId());
        po.setClassCode(domain.getClassCode());
        po.setClassName(domain.getClassName());
        po.setOrgUnitId(domain.getOrgUnitId());
        po.setGradeId(domain.getGradeId());
        po.setEnrollmentYear(domain.getEnrollmentYear());
        po.setGradeLevel(domain.getGradeLevel());
        po.setMajorDirectionId(domain.getMajorDirectionId());
        po.setStudentCount(domain.getCurrentSize());
        po.setStatus(toDbStatus(domain.getStatus()));

        // 保存班主任信息
        domain.getCurrentHeadTeacher().ifPresent(teacher ->
            po.setTeacherId(teacher.getTeacherId())
        );

        // 保存副班主任信息（取第一个副班主任）
        List<TeacherAssignment> deputies = domain.getCurrentDeputyHeadTeachers();
        if (!deputies.isEmpty()) {
            po.setAssistantTeacherId(deputies.get(0).getTeacherId());
        }

        return po;
    }

    private SchoolClass toDomain(SchoolClassPO po) {
        // 处理gradeLevel: null、<=0 或 >10 时默认为1
        Integer gradeLevel = po.getGradeLevel();
        if (gradeLevel == null || gradeLevel < 1 || gradeLevel > 10) {
            gradeLevel = 1;
        }

        // 加载教师任职信息
        List<TeacherAssignment> teacherAssignments = new ArrayList<>();

        // 加载班主任
        if (po.getTeacherId() != null) {
            String teacherName = getTeacherName(po.getTeacherId());
            if (teacherName != null) {
                teacherAssignments.add(TeacherAssignment.create(
                    po.getTeacherId(),
                    teacherName,
                    TeacherAssignment.TeacherRole.HEAD_TEACHER,
                    LocalDate.now()
                ));
            }
        }

        // 加载副班主任
        if (po.getAssistantTeacherId() != null) {
            String assistantName = getTeacherName(po.getAssistantTeacherId());
            if (assistantName != null) {
                teacherAssignments.add(TeacherAssignment.create(
                    po.getAssistantTeacherId(),
                    assistantName,
                    TeacherAssignment.TeacherRole.DEPUTY_HEAD_TEACHER,
                    LocalDate.now()
                ));
            }
        }

        return SchoolClass.builder()
            .id(po.getId())
            .classCode(po.getClassCode())
            .className(po.getClassName())
            .shortName(null)
            .orgUnitId(po.getOrgUnitId())
            .gradeId(po.getGradeId())
            .enrollmentYear(po.getEnrollmentYear())
            .gradeLevel(gradeLevel)
            .majorDirectionId(po.getMajorDirectionId())
            .schoolingYears(3)
            .standardSize(po.getStandardSize() != null ? po.getStandardSize() : 50)
            .currentSize(po.getStudentCount() != null ? po.getStudentCount() : 0)
            .status(fromDbStatus(po.getStatus()))
            .teacherAssignments(teacherAssignments)
            .sortOrder(0)
            .build();
    }

    private String getTeacherName(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        UserPO user = userDomainMapper.selectById(teacherId);
        return user != null ? user.getRealName() : null;
    }

    private String toOrgUnitStatus(ClassStatus status) {
        if (status == null) return "DRAFT";
        return switch (status) {
            case ACTIVE -> "ACTIVE";
            case GRADUATED, DISSOLVED -> "DISSOLVED";
            default -> "DRAFT";
        };
    }

    /** @deprecated kept for classes table which still uses Integer status */
    private Integer toDbStatus(ClassStatus status) {
        if (status == null) return 0;
        return status == ClassStatus.ACTIVE ? 1 : 0;
    }

    private ClassStatus fromDbStatus(Integer status) {
        if (status == null || status == 0) return ClassStatus.PREPARING;
        return ClassStatus.ACTIVE;
    }
}
