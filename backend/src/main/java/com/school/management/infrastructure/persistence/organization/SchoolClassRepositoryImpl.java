package com.school.management.infrastructure.persistence.organization;

import com.school.management.domain.organization.model.ClassStatus;
import com.school.management.domain.organization.model.SchoolClass;
import com.school.management.domain.organization.model.TeacherAssignment;
import com.school.management.domain.organization.repository.SchoolClassRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository implementation for SchoolClass.
 * Maps to the existing 'classes' table.
 */
@Repository
public class SchoolClassRepositoryImpl implements SchoolClassRepository {

    private final SchoolClassMapper classMapper;

    public SchoolClassRepositoryImpl(SchoolClassMapper classMapper) {
        this.classMapper = classMapper;
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
        SchoolClassPO po = toPO(schoolClass);

        if (schoolClass.getId() == null) {
            classMapper.insert(po);
            schoolClass.setId(po.getId());
        } else {
            classMapper.updateById(po);
        }

        return schoolClass;
    }

    @Override
    public void delete(SchoolClass aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            classMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        classMapper.deleteById(id);
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
        // orgUnitId maps to departmentId in actual table
        return classMapper.findByDepartmentId(orgUnitId).stream()
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
        return classMapper.findAllEnabled().stream()
            .filter(po -> dbStatus.equals(po.getStatus()))
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SchoolClass> findByOrgUnitIdAndEnrollmentYear(Long orgUnitId, Integer enrollmentYear) {
        return classMapper.findByDepartmentId(orgUnitId).stream()
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
    public List<SchoolClass> findByMajorDirectionId(Long majorDirectionId) {
        return classMapper.findAllEnabled().stream()
            .filter(po -> majorDirectionId.equals(po.getMajorDirectionId()))
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByClassCode(String classCode) {
        return classMapper.findByClassCode(classCode) != null;
    }

    @Override
    public int countByOrgUnitId(Long orgUnitId) {
        return classMapper.countByDepartmentId(orgUnitId);
    }

    @Override
    public int countByStatus(ClassStatus status) {
        return classMapper.countByStatus(toDbStatus(status));
    }

    @Override
    public List<SchoolClass> findGraduatingClasses(Integer graduationYear) {
        // Filter by graduation year from all classes
        return classMapper.findAllEnabled().stream()
            .filter(po -> graduationYear.equals(po.getGraduationYear()))
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    private SchoolClassPO toPO(SchoolClass domain) {
        SchoolClassPO po = new SchoolClassPO();
        po.setId(domain.getId());
        po.setClassCode(domain.getClassCode());
        po.setClassName(domain.getClassName());
        po.setDepartmentId(domain.getOrgUnitId());
        po.setGradeId(domain.getGradeId());
        po.setEnrollmentYear(domain.getEnrollmentYear());
        po.setGradeLevel(domain.getGradeLevel());
        po.setMajorDirectionId(domain.getMajorDirectionId());
        po.setStudentCount(domain.getCurrentSize());
        po.setStatus(toDbStatus(domain.getStatus()));
        return po;
    }

    private SchoolClass toDomain(SchoolClassPO po) {
        // 处理gradeLevel: null、<=0 或 >10 时默认为1
        // 数据库中可能存储了错误的值（如2023），需要转换为有效的年级(1-10)
        Integer gradeLevel = po.getGradeLevel();
        if (gradeLevel == null || gradeLevel < 1 || gradeLevel > 10) {
            gradeLevel = 1;
        }

        return SchoolClass.builder()
            .id(po.getId())
            .classCode(po.getClassCode())
            .className(po.getClassName())
            .shortName(null) // Not in database
            .orgUnitId(po.getDepartmentId())
            .gradeId(po.getGradeId())
            .enrollmentYear(po.getEnrollmentYear())
            .gradeLevel(gradeLevel)
            .majorDirectionId(po.getMajorDirectionId())
            .schoolingYears(3) // Default, not in database
            .standardSize(45) // Default, not in database
            .currentSize(po.getStudentCount() != null ? po.getStudentCount() : 0)
            .status(fromDbStatus(po.getStatus()))
            .teacherAssignments(new ArrayList<>()) // Not loaded from DB
            .sortOrder(0) // Not in database
            .build();
    }

    private Integer toDbStatus(ClassStatus status) {
        if (status == null) return 0;
        return status == ClassStatus.ACTIVE ? 1 : 0;
    }

    private ClassStatus fromDbStatus(Integer status) {
        if (status == null || status == 0) return ClassStatus.PREPARING;
        return ClassStatus.ACTIVE;
    }
}
