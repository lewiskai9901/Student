package com.school.management.infrastructure.persistence.calendar;

import com.school.management.domain.calendar.model.entity.TeachingWeek;
import com.school.management.domain.calendar.repository.TeachingWeekRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TeachingWeekRepositoryImpl implements TeachingWeekRepository {

    private final TeachingWeekMapper mapper;

    public TeachingWeekRepositoryImpl(TeachingWeekMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TeachingWeek save(TeachingWeek week) {
        TeachingWeekPO po = toPO(week);
        if (week.getId() == null) {
            mapper.insert(po);
            week.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return week;
    }

    @Override
    public List<TeachingWeek> findBySemesterId(Long semesterId) {
        return mapper.findBySemesterId(semesterId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteBySemesterId(Long semesterId) {
        mapper.deleteBySemesterId(semesterId);
    }

    private TeachingWeekPO toPO(TeachingWeek d) {
        TeachingWeekPO po = new TeachingWeekPO();
        po.setId(d.getId());
        po.setSemesterId(d.getSemesterId());
        po.setWeekNumber(d.getWeekNumber());
        po.setWeekName(d.getWeekName());
        po.setStartDate(d.getStartDate());
        po.setEndDate(d.getEndDate());
        po.setWeekType(d.getWeekType());
        po.setIsCurrent(d.getIsCurrent() != null && d.getIsCurrent() ? 1 : 0);
        po.setStatus(d.getStatus());
        return po;
    }

    private TeachingWeek toDomain(TeachingWeekPO po) {
        return TeachingWeek.reconstruct(po.getId(), po.getSemesterId(), po.getWeekNumber(),
                po.getWeekName(), po.getStartDate(), po.getEndDate(), po.getWeekType(),
                po.getIsCurrent() != null && po.getIsCurrent() == 1, po.getStatus());
    }
}
