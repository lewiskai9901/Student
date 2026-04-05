package com.school.management.infrastructure.persistence.student;

import com.school.management.domain.student.model.Cohort;
import com.school.management.domain.student.model.CohortStatus;
import com.school.management.domain.student.repository.CohortRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of CohortRepository.
 * Maps to the existing 'grades' table.
 */
@Repository
public class CohortRepositoryImpl implements CohortRepository {

    private final CohortPersistenceMapper cohortMapper;

    public CohortRepositoryImpl(CohortPersistenceMapper cohortMapper) {
        this.cohortMapper = cohortMapper;
    }

    @Override
    public Optional<Cohort> findById(Long id) {
        CohortPO po = cohortMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Cohort save(Cohort aggregate) {
        CohortPO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            cohortMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            cohortMapper.updateById(po);
        }
        return aggregate;
    }

    @Override
    public void delete(Cohort aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            cohortMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        cohortMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return cohortMapper.selectById(id) != null;
    }

    @Override
    public Optional<Cohort> findByGradeCode(String gradeCode) {
        CohortPO po = cohortMapper.findByGradeCode(gradeCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<Cohort> findByEnrollmentYear(Integer enrollmentYear) {
        CohortPO po = cohortMapper.findByEnrollmentYear(enrollmentYear);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Cohort> findByStatus(CohortStatus status) {
        return cohortMapper.findByStatus(status.toLegacyStatus()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Cohort> findAll() {
        return cohortMapper.findAllOrderByEnrollmentYearDesc().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Cohort> findActiveCohorts() {
        return cohortMapper.findActiveCohorts().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Cohort> findByDirectorId(Long directorId) {
        return cohortMapper.findByDirectorId(directorId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByGradeCode(String gradeCode) {
        return cohortMapper.existsByGradeCode(gradeCode);
    }

    @Override
    public boolean existsByEnrollmentYear(Integer enrollmentYear) {
        return cohortMapper.existsByEnrollmentYear(enrollmentYear);
    }

    @Override
    public int countByStatus(CohortStatus status) {
        return cohortMapper.countByStatus(status.toLegacyStatus());
    }

    @Override
    public List<Cohort> findGraduatingCohorts(Integer graduationYear) {
        return cohortMapper.findGraduatingCohorts(graduationYear).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Cohort> findAllOrderByEnrollmentYearDesc() {
        return cohortMapper.findAllOrderByEnrollmentYearDesc().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    // Conversion methods
    private Cohort toDomain(CohortPO po) {
        return Cohort.builder()
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
            .status(CohortStatus.fromLegacyStatus(po.getStatus()))
            .sortOrder(po.getSortOrder())
            .remarks(po.getRemarks())
            .createdBy(po.getCreatedBy())
            .build();
    }

    private CohortPO toPO(Cohort domain) {
        CohortPO po = new CohortPO();
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
