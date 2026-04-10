package com.school.management.infrastructure.persistence.calendar;

import com.school.management.domain.calendar.model.aggregate.Semester;
import com.school.management.domain.calendar.model.valueobject.SemesterStatus;
import com.school.management.domain.calendar.model.valueobject.SemesterType;
import com.school.management.domain.calendar.repository.SemesterRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SemesterRepositoryImpl implements SemesterRepository {

    private final SemesterMapper semesterMapper;

    public SemesterRepositoryImpl(SemesterMapper semesterMapper) {
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
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public Optional<Semester> findBySemesterCode(String code) {
        SemesterPO po = semesterMapper.findBySemesterCode(code);
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public Optional<Semester> findCurrentSemester() {
        SemesterPO po = semesterMapper.findCurrentSemester();
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public boolean existsBySemesterCode(String code) {
        return semesterMapper.existsBySemesterCode(code);
    }

    @Override
    public boolean existsBySemesterCodeAndIdNot(String code, Long excludeId) {
        return semesterMapper.existsBySemesterCodeAndIdNot(code, excludeId);
    }

    @Override
    public List<Semester> findByDateRange(LocalDate start, LocalDate end) {
        return semesterMapper.findByDateRange(start, end).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Semester> findByStartYear(Integer year) {
        return semesterMapper.findByStartYear(year).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Semester> findByAcademicYearId(Long yearId) {
        return semesterMapper.findByAcademicYearId(yearId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Semester> findAllActive() {
        return semesterMapper.findAllActive().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Semester> findAllOrderByStartDateDesc() {
        return semesterMapper.findAllOrderByStartDateDesc().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(Semester semester) {
        if (semester != null && semester.getId() != null) semesterMapper.deleteById(semester.getId());
    }

    @Override
    public void deleteById(Long id) {
        semesterMapper.deleteById(id);
    }

    @Override
    public List<Semester> findAll(int page, int size) {
        return semesterMapper.findAllPaged((page - 1) * size, size).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long count() {
        return semesterMapper.countAll();
    }

    @Override
    public void clearAllCurrentFlags() {
        semesterMapper.clearAllCurrentFlags();
    }

    private SemesterPO toPO(Semester d) {
        SemesterPO po = new SemesterPO();
        po.setId(d.getId());
        po.setAcademicYearId(d.getAcademicYearId());
        po.setSemesterName(d.getSemesterName());
        po.setSemesterCode(d.getSemesterCode());
        po.setStartDate(d.getStartDate());
        po.setEndDate(d.getEndDate());
        po.setStartYear(d.getStartYear());
        po.setSemesterType(d.getSemesterType() != null ? d.getSemesterType().getCode() : null);
        po.setIsCurrent(d.getIsCurrent() != null && d.getIsCurrent() ? 1 : 0);
        po.setStatus(d.getStatus() != null ? d.getStatus().getCode() : null);
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        po.setCreatedBy(d.getCreatedBy());
        po.setUpdatedBy(d.getUpdatedBy());
        return po;
    }

    private Semester toDomain(SemesterPO po) {
        return Semester.reconstruct(po.getId(), po.getAcademicYearId(),
                po.getSemesterName(), po.getSemesterCode(),
                po.getStartDate(), po.getEndDate(), po.getStartYear(),
                po.getSemesterType() != null ? SemesterType.fromCode(po.getSemesterType()) : null,
                po.getIsCurrent() != null && po.getIsCurrent() == 1,
                po.getStatus() != null ? SemesterStatus.fromCode(po.getStatus()) : null,
                po.getCreatedAt(), po.getUpdatedAt(), po.getCreatedBy(), po.getUpdatedBy());
    }
}
