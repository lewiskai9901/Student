package com.school.management.application.calendar.query;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarDayDTO {
    private String date;
    private int weekday;
    private String dayType;
    private String eventName;
    private Integer followWeekday;
    private Long eventId;
}
