package com.school.management.domain.schedule.service;

import java.time.LocalDate;
import java.util.List;

public interface HolidayService {
    boolean isHoliday(LocalDate date);
    boolean isExcluded(LocalDate date, List<String> excludedDates);
}
