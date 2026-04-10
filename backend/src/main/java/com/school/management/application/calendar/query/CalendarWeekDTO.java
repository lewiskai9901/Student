package com.school.management.application.calendar.query;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CalendarWeekDTO {
    private int weekNumber;
    private String startDate;
    private String endDate;
    private String weekType; // TEACHING / EXAM / VACATION
    private List<CalendarDayDTO> days;
}
