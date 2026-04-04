package com.school.management.infrastructure.persistence.calendar;

import com.school.management.domain.calendar.model.aggregate.AcademicYear;
import com.school.management.domain.calendar.repository.AcademicYearRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AcademicYearRepositoryImpl implements AcademicYearRepository {

    private final AcademicYearMapper mapper;

    public AcademicYearRepositoryImpl(AcademicYearMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AcademicYear save(AcademicYear year) {
        AcademicYearPO po = toPO(year);
        if (year.getId() == null) {
            mapper.insert(po);
            year.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return year;
    }

    @Override
    public Optional<AcademicYear> findById(Long id) {
        AcademicYearPO po = mapper.selectById(id);
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public Optional<AcademicYear> findCurrent() {
        AcademicYearPO po = mapper.findCurrent();
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public List<AcademicYear> findAllActive() {
        return mapper.findAllActive().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void clearAllCurrentFlags() {
        mapper.clearAllCurrentFlags();
    }

    private AcademicYearPO toPO(AcademicYear d) {
        AcademicYearPO po = new AcademicYearPO();
        po.setId(d.getId());
        po.setYearCode(d.getYearCode());
        po.setYearName(d.getYearName());
        po.setStartDate(d.getStartDate());
        po.setEndDate(d.getEndDate());
        po.setIsCurrent(d.getIsCurrent() != null && d.getIsCurrent() ? 1 : 0);
        po.setStatus(d.getStatus());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private AcademicYear toDomain(AcademicYearPO po) {
        return AcademicYear.reconstruct(po.getId(), po.getYearCode(), po.getYearName(),
                po.getStartDate(), po.getEndDate(),
                po.getIsCurrent() != null && po.getIsCurrent() == 1,
                po.getStatus(), po.getCreatedAt(), po.getUpdatedAt());
    }
}
