package com.school.management.infrastructure.extension.plugins.education.application.calendar.query;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CalendarGridDTO {
    private List<CalendarWeekDTO> weeks;
    private int totalTeachingDays;
    private int totalHolidayDays;
    private int totalMakeupDays;
    private int totalExamDays;
}
