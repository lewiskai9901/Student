package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.platform.HolidayCalendar;
import com.school.management.domain.inspection.repository.v7.HolidayCalendarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class HolidayCalendarApplicationService {

    private final HolidayCalendarRepository calendarRepository;

    // ========== CRUD ==========

    @Transactional
    public HolidayCalendar create(String calendarName, Integer year, String holidays,
                                   String workdays, Boolean isDefault) {
        HolidayCalendar calendar = HolidayCalendar.create(HolidayCalendar.builder()
                .calendarName(calendarName)
                .year(year)
                .holidays(holidays)
                .workdays(workdays)
                .isDefault(isDefault));
        HolidayCalendar saved = calendarRepository.save(calendar);
        log.info("Created holiday calendar: name={}, year={}", calendarName, year);
        return saved;
    }

    @Transactional
    public HolidayCalendar update(Long id, String calendarName, Integer year, String holidays,
                                   String workdays, Boolean isDefault) {
        HolidayCalendar calendar = calendarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("假日日历不存在: " + id));
        calendar.update(calendarName, year, holidays, workdays, isDefault);
        HolidayCalendar saved = calendarRepository.save(calendar);
        log.info("Updated holiday calendar: id={}, name={}", id, calendarName);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        calendarRepository.deleteById(id);
        log.info("Deleted holiday calendar: id={}", id);
    }

    @Transactional(readOnly = true)
    public HolidayCalendar findById(Long id) {
        return calendarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("假日日历不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<HolidayCalendar> findByYear(Integer year) {
        return calendarRepository.findByYear(year);
    }

    @Transactional(readOnly = true)
    public HolidayCalendar findDefault() {
        return calendarRepository.findDefault()
                .orElseThrow(() -> new IllegalArgumentException("默认假日日历不存在"));
    }

    @Transactional(readOnly = true)
    public List<HolidayCalendar> findAll() {
        return calendarRepository.findAll();
    }
}
