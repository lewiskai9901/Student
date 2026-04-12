package com.school.management.domain.calendar.model.entity;

import java.time.LocalDate;

public class TeachingWeek {
    private Long id;
    private Long semesterId;
    private Integer weekNumber;
    private String weekName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer weekType; // 1=教学 2=考试 3=假期
    private Boolean isCurrent;
    private Integer status; // 1=active, 0=inactive

    protected TeachingWeek() {}

    public static TeachingWeek create(Long semesterId, Integer weekNumber, String weekName,
                                       LocalDate startDate, LocalDate endDate) {
        if (semesterId == null) throw new IllegalArgumentException("学期ID不能为空");
        if (weekNumber == null || weekNumber < 1) throw new IllegalArgumentException("周次必须大于0");
        validateDates(startDate, endDate);
        TeachingWeek week = new TeachingWeek();
        week.semesterId = semesterId;
        week.weekNumber = weekNumber;
        week.weekName = weekName != null ? weekName : "第" + weekNumber + "周";
        week.startDate = startDate;
        week.endDate = endDate;
        week.weekType = 1; // 默认教学周
        week.isCurrent = false;
        week.status = 1;
        return week;
    }

    public static TeachingWeek reconstruct(Long id, Long semesterId, Integer weekNumber, String weekName,
                                            LocalDate startDate, LocalDate endDate, Integer weekType,
                                            Boolean isCurrent, Integer status) {
        TeachingWeek week = new TeachingWeek();
        week.id = id;
        week.semesterId = semesterId;
        week.weekNumber = weekNumber;
        week.weekName = weekName;
        week.startDate = startDate;
        week.endDate = endDate;
        week.weekType = weekType != null ? weekType : 1;
        week.isCurrent = isCurrent;
        week.status = status;
        return week;
    }

    private static void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) throw new IllegalArgumentException("教学周日期不能为空");
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("结束日期不能早于开始日期");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSemesterId() { return semesterId; }
    public Integer getWeekNumber() { return weekNumber; }
    public String getWeekName() { return weekName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getWeekType() { return weekType; }
    public Boolean getIsCurrent() { return isCurrent; }
    public Integer getStatus() { return status; }
}
