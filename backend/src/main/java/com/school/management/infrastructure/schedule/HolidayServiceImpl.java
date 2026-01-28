package com.school.management.infrastructure.schedule;

import com.school.management.domain.schedule.service.HolidayService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HolidayServiceImpl implements HolidayService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public boolean isHoliday(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    @Override
    public boolean isExcluded(LocalDate date, List<String> excludedDates) {
        if (excludedDates == null || excludedDates.isEmpty()) {
            return false;
        }
        String dateStr = date.format(DATE_FMT);
        return excludedDates.contains(dateStr);
    }
}
