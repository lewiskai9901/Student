package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.platform.HolidayCalendar;

import java.util.List;
import java.util.Optional;

public interface HolidayCalendarRepository {

    HolidayCalendar save(HolidayCalendar calendar);

    Optional<HolidayCalendar> findById(Long id);

    List<HolidayCalendar> findByYear(Integer year);

    Optional<HolidayCalendar> findDefault();

    List<HolidayCalendar> findAll();

    void deleteById(Long id);
}
