package com.school.management.infrastructure.persistence.organization;

import com.school.management.domain.organization.model.Grade;
import com.school.management.domain.organization.model.GradeStatus;
import com.school.management.domain.organization.repository.GradeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of GradeRepository.
 * Maps to the existing 'grades' table.
 */
@Repository
public class GradeRepositoryImpl implements GradeRepository {

    private final GradeMapper gradeMapper;

    public GradeRepositoryImpl(GradeMapper gradeMapper) {
        this.gradeMapper = gradeMapper;
    }

    @Override
    public Optional<Grade> findById(Long id) {
        GradePO po = gradeMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Grade save(Grade aggregate) {
        GradePO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            gradeMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            gradeMapper.updateById(po);
        }
        return aggregate;
    }

    @Override
    public void delete(Grade aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            gradeMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        gradeMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return gradeMapper.selectById(id) != null;
    }

    @Override
    public Optional<Grade> findByGradeCode(String gradeCode) {
        GradePO po = gradeMapper.findByGradeCode(gradeCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<Grade> findByEnrollmentYear(Integer enrollmentYear) {
        GradePO po = gradeMapper.findByEnrollmentYear(enrollmentYear);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Grade> findByStatus(GradeStatus status) {
        return gradeMapper.findByStatus(status.toLegacyStatus()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Grade> findAll() {
        return gradeMapper.findAllOrderByEnrollmentYearDesc().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Grade> findActiveGrades() {
        return gradeMapper.findActiveGrades().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Grade> findByDirectorId(Long directorId) {
        return gradeMapper.findByDirectorId(directorId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByGradeCode(String gradeCode) {
        return gradeMapper.existsByGradeCode(gradeCode);
    }

    @Override
    public boolean existsByEnrollmentYear(Integer enrollmentYear) {
        return gradeMapper.existsByEnrollmentYear(enrollmentYear);
    }

    @Override
    public int countByStatus(GradeStatus status) {
        return gradeMapper.countByStatus(status.toLegacyStatus());
    }

    @Override
    public List<Grade> findGraduatingGrades(Integer graduationYear) {
        return gradeMapper.findGraduatingGrades(graduationYear).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Grade> findAllOrderByEnrollmentYearDesc() {
        return gradeMapper.findAllOrderByEnrollmentYearDesc().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    // Conversion methods
    private Grade toDomain(GradePO po) {
        return Grade.builder()
            .id(po.getId())
            .gradeCode(po.getGradeCode())
            .gradeName(po.getGradeName())
            .enrollmentYear(po.getEnrollmentYear())
            .graduationYear(po.getGraduationYear())
            .schoolingYears(calculateSchoolingYears(po.getEnrollmentYear(), po.getGraduationYear()))
            .directorId(po.getGradeDirectorId())
            .directorName(null) // Will be fetched separately if needed
            .counselorId(po.getGradeCounselorId())
            .counselorName(null) // Will be fetched separately if needed
            .standardClassSize(po.getStandardClassSize())
            .status(GradeStatus.fromLegacyStatus(po.getStatus()))
            .sortOrder(po.getSortOrder())
            .remarks(po.getRemarks())
            .createdBy(po.getCreatedBy())
            .build();
    }

    private GradePO toPO(Grade domain) {
        GradePO po = new GradePO();
        po.setId(domain.getId());
        po.setGradeCode(domain.getGradeCode());
        po.setGradeName(domain.getGradeName());
        po.setEnrollmentYear(domain.getEnrollmentYear());
        po.setGraduationYear(domain.getGraduationYear());
        po.setGradeDirectorId(domain.getDirectorId());
        po.setGradeCounselorId(domain.getCounselorId());
        po.setStandardClassSize(domain.getStandardClassSize());
        po.setStatus(domain.getStatus().toLegacyStatus());
        po.setSortOrder(domain.getSortOrder());
        po.setRemarks(domain.getRemarks());
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedBy(domain.getUpdatedBy());
        return po;
    }

    private Integer calculateSchoolingYears(Integer enrollmentYear, Integer graduationYear) {
        if (enrollmentYear == null || graduationYear == null) {
            return 3; // Default
        }
        return graduationYear - enrollmentYear;
    }
}
