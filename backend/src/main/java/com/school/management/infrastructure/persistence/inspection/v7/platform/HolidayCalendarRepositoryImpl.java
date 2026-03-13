package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.school.management.domain.inspection.model.v7.platform.HolidayCalendar;
import com.school.management.domain.inspection.repository.v7.HolidayCalendarRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class HolidayCalendarRepositoryImpl implements HolidayCalendarRepository {

    private final HolidayCalendarMapper mapper;

    public HolidayCalendarRepositoryImpl(HolidayCalendarMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public HolidayCalendar save(HolidayCalendar calendar) {
        HolidayCalendarPO po = toPO(calendar);
        if (calendar.getId() == null) {
            mapper.insert(po);
            calendar.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return calendar;
    }

    @Override
    public Optional<HolidayCalendar> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<HolidayCalendar> findByYear(Integer year) {
        return mapper.findByYear(year).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<HolidayCalendar> findDefault() {
        return Optional.ofNullable(mapper.findDefault()).map(this::toDomain);
    }

    @Override
    public List<HolidayCalendar> findAll() {
        return mapper.selectList(null).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private HolidayCalendarPO toPO(HolidayCalendar d) {
        HolidayCalendarPO po = new HolidayCalendarPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId());
        po.setCalendarName(d.getCalendarName());
        po.setYear(d.getYear());
        po.setHolidays(d.getHolidays());
        po.setWorkdays(d.getWorkdays());
        po.setIsDefault(d.getIsDefault());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private HolidayCalendar toDomain(HolidayCalendarPO po) {
        return HolidayCalendar.reconstruct(HolidayCalendar.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .calendarName(po.getCalendarName())
                .year(po.getYear())
                .holidays(po.getHolidays())
                .workdays(po.getWorkdays())
                .isDefault(po.getIsDefault())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
