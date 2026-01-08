package com.school.management.infrastructure.persistence.semester;

import com.school.management.domain.semester.model.aggregate.Semester;
import com.school.management.domain.semester.model.valueobject.SemesterStatus;
import com.school.management.domain.semester.model.valueobject.SemesterType;
import com.school.management.domain.semester.repository.SemesterRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of SemesterRepository
 */
@Repository
public class SemesterRepositoryImpl implements SemesterRepository {

    private final SemesterDomainMapper semesterMapper;

    public SemesterRepositoryImpl(SemesterDomainMapper semesterMapper) {
        this.semesterMapper = semesterMapper;
    }

    @Override
    public Semester save(Semester semester) {
        SemesterPO po = toPO(semester);

        if (semester.getId() == null) {
            semesterMapper.insert(po);
            semester.setId(po.getId());
        } else {
            semesterMapper.updateById(po);
        }

        return semester;
    }

    @Override
    public Optional<Semester> findById(Long id) {
        SemesterPO po = semesterMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<Semester> findBySemesterCode(String semesterCode) {
        SemesterPO po = semesterMapper.findBySemesterCode(semesterCode);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<Semester> findCurrentSemester() {
        SemesterPO po = semesterMapper.findCurrentSemester();
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public boolean existsBySemesterCode(String semesterCode) {
        return semesterMapper.existsBySemesterCode(semesterCode);
    }

    @Override
    public boolean existsBySemesterCodeAndIdNot(String semesterCode, Long excludeId) {
        return semesterMapper.existsBySemesterCodeAndIdNot(semesterCode, excludeId);
    }

    @Override
    public List<Semester> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return semesterMapper.findByDateRange(startDate, endDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Semester> findByStartYear(Integer startYear) {
        return semesterMapper.findByStartYear(startYear).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Semester> findAllActive() {
        return semesterMapper.findAllActive().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Semester> findAllOrderByStartDateDesc() {
        return semesterMapper.findAllOrderByStartDateDesc().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        semesterMapper.deleteById(id);
    }

    @Override
    public List<Semester> findAll(int page, int size) {
        int offset = (page - 1) * size;
        return semesterMapper.findAllPaged(offset, size).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return semesterMapper.countAll();
    }

    @Override
    public void clearAllCurrentFlags() {
        semesterMapper.clearAllCurrentFlags();
    }

    // ==================== Mapping Methods ====================

    private SemesterPO toPO(Semester domain) {
        SemesterPO po = new SemesterPO();
        po.setId(domain.getId());
        po.setSemesterName(domain.getSemesterName());
        po.setSemesterCode(domain.getSemesterCode());
        po.setStartDate(domain.getStartDate());
        po.setEndDate(domain.getEndDate());
        po.setStartYear(domain.getStartYear());
        po.setSemesterType(domain.getSemesterType() != null ? domain.getSemesterType().getCode() : null);
        po.setIsCurrent(domain.getIsCurrent() != null && domain.getIsCurrent() ? 1 : 0);
        po.setStatus(domain.getStatus() != null ? domain.getStatus().getCode() : null);
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedBy(domain.getUpdatedBy());
        return po;
    }

    private Semester toDomain(SemesterPO po) {
        return Semester.reconstruct(
                po.getId(),
                po.getSemesterName(),
                po.getSemesterCode(),
                po.getStartDate(),
                po.getEndDate(),
                po.getStartYear(),
                po.getSemesterType() != null ? SemesterType.fromCode(po.getSemesterType()) : null,
                po.getIsCurrent() != null && po.getIsCurrent() == 1,
                po.getStatus() != null ? SemesterStatus.fromCode(po.getStatus()) : null,
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getCreatedBy(),
                po.getUpdatedBy()
        );
    }
}
