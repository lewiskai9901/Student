package com.school.management.infrastructure.persistence.calendar;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.calendar.model.entity.AcademicEvent;
import com.school.management.domain.calendar.model.valueobject.EventType;
import com.school.management.domain.calendar.repository.AcademicEventRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AcademicEventRepositoryImpl implements AcademicEventRepository {

    private final AcademicEventMapper mapper;

    public AcademicEventRepositoryImpl(AcademicEventMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AcademicEvent save(AcademicEvent event) {
        AcademicEventPO po = toPO(event);
        if (event.getId() == null) {
            mapper.insert(po);
            event.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return event;
    }

    @Override
    public Optional<AcademicEvent> findById(Long id) {
        AcademicEventPO po = mapper.selectById(id);
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public List<AcademicEvent> findAll(Long yearId, Long semesterId, Integer eventType) {
        LambdaQueryWrapper<AcademicEventPO> wrapper = new LambdaQueryWrapper<>();
        if (yearId != null) wrapper.eq(AcademicEventPO::getYearId, yearId);
        if (semesterId != null) wrapper.eq(AcademicEventPO::getSemesterId, semesterId);
        if (eventType != null) wrapper.eq(AcademicEventPO::getEventType, eventType);
        wrapper.orderByAsc(AcademicEventPO::getStartDate);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private AcademicEventPO toPO(AcademicEvent d) {
        AcademicEventPO po = new AcademicEventPO();
        po.setId(d.getId());
        po.setYearId(d.getYearId());
        po.setSemesterId(d.getSemesterId());
        po.setEventName(d.getEventName());
        po.setEventType(d.getEventType() != null ? d.getEventType().getCode() : 5);
        po.setStartDate(d.getStartDate());
        po.setEndDate(d.getEndDate());
        po.setAllDay(d.getAllDay() != null && d.getAllDay() ? 1 : 0);
        po.setDescription(d.getDescription());
        return po;
    }

    private AcademicEvent toDomain(AcademicEventPO po) {
        return AcademicEvent.reconstruct(po.getId(), po.getYearId(), po.getSemesterId(),
                po.getEventName(), EventType.fromCode(po.getEventType()),
                po.getStartDate(), po.getEndDate(),
                po.getAllDay() != null && po.getAllDay() == 1,
                po.getDescription());
    }
}
