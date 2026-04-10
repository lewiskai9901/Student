package com.school.management.application.calendar;

import com.school.management.application.calendar.command.*;
import com.school.management.application.calendar.query.CalendarDayDTO;
import com.school.management.application.calendar.query.CalendarGridDTO;
import com.school.management.application.calendar.query.CalendarWeekDTO;
import com.school.management.domain.calendar.model.aggregate.AcademicYear;
import com.school.management.domain.calendar.model.aggregate.Semester;
import com.school.management.domain.calendar.model.entity.AcademicEvent;
import com.school.management.domain.calendar.model.entity.TeachingWeek;
import com.school.management.domain.calendar.model.valueobject.EventType;
import com.school.management.domain.calendar.model.valueobject.SemesterType;
import com.school.management.domain.calendar.repository.AcademicEventRepository;
import com.school.management.domain.calendar.repository.AcademicYearRepository;
import com.school.management.domain.calendar.repository.SemesterRepository;
import com.school.management.domain.calendar.repository.TeachingWeekRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarApplicationService {

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final TeachingWeekRepository teachingWeekRepository;
    private final AcademicEventRepository academicEventRepository;
    private final DomainEventPublisher eventPublisher;

    // ==================== 学年管理 ====================

    @Transactional(readOnly = true)
    public List<AcademicYear> listAcademicYears() {
        return academicYearRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public Optional<AcademicYear> getAcademicYear(Long id) {
        return academicYearRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<AcademicYear> getCurrentAcademicYear() {
        return academicYearRepository.findCurrent();
    }

    @Transactional
    public AcademicYear createAcademicYear(CreateAcademicYearCommand command) {
        log.info("创建学年: {}", command.getYearName());
        AcademicYear year = AcademicYear.create(
                command.getYearCode(), command.getYearName(),
                command.getStartDate(), command.getEndDate());
        return academicYearRepository.save(year);
    }

    @Transactional
    public AcademicYear updateAcademicYear(Long id, UpdateAcademicYearCommand command) {
        AcademicYear year = academicYearRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学年不存在: " + id));
        year.updateBasicInfo(command.getYearName(), command.getStartDate(), command.getEndDate());
        return academicYearRepository.save(year);
    }

    @Transactional
    public void deleteAcademicYear(Long id) {
        academicYearRepository.deleteById(id);
    }

    @Transactional
    public void setCurrentAcademicYear(Long id) {
        academicYearRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学年不存在: " + id));
        academicYearRepository.clearAllCurrentFlags();
        AcademicYear year = academicYearRepository.findById(id).get();
        year.setAsCurrent();
        academicYearRepository.save(year);
    }

    // ==================== 学期管理 ====================

    @Transactional(readOnly = true)
    public List<Semester> listSemesters() {
        return semesterRepository.findAllOrderByStartDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Semester> listSemestersByYear(Long yearId) {
        return semesterRepository.findByAcademicYearId(yearId);
    }

    @Transactional(readOnly = true)
    public Optional<Semester> getSemester(Long id) {
        return semesterRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Semester> getCurrentSemester() {
        return semesterRepository.findCurrentSemester();
    }

    @Transactional
    public Semester createSemester(CreateSemesterCommand command) {
        log.info("创建学期: {}", command.getSemesterName());

        // 验证学年存在（必填）
        if (command.getAcademicYearId() == null) {
            throw new BusinessException("必须选择所属学年");
        }
        AcademicYear year = academicYearRepository.findById(command.getAcademicYearId())
                .orElseThrow(() -> new BusinessException("学年不存在: " + command.getAcademicYearId()));

        SemesterType semesterType = command.getSemesterType() != null
                ? SemesterType.fromCode(command.getSemesterType()) : SemesterType.FIRST;
        Integer startYear = command.getStartDate().getYear();
        String semesterCode = command.getSemesterCode() != null
                ? command.getSemesterCode()
                : Semester.generateCode(startYear, semesterType);

        if (semesterRepository.existsBySemesterCode(semesterCode)) {
            throw new BusinessException("学期编码已存在: " + semesterCode);
        }

        Semester semester = Semester.create(command.getAcademicYearId(), command.getSemesterName(),
                semesterCode, command.getStartDate(), command.getEndDate(), startYear, semesterType);
        semester = semesterRepository.save(semester);
        publishEvents(semester);
        return semester;
    }

    @Transactional
    public Semester updateSemester(Long id, UpdateSemesterCommand command) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学期不存在: " + id));
        semester.updateBasicInfo(command.getSemesterName(), command.getStartDate(), command.getEndDate());
        semester = semesterRepository.save(semester);
        publishEvents(semester);
        return semester;
    }

    @Transactional
    public void deleteSemester(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学期不存在: " + id));
        if (semester.getIsCurrent() != null && semester.getIsCurrent()) {
            throw new BusinessException("不能删除当前学期");
        }
        semesterRepository.deleteById(id);
    }

    @Transactional
    public Semester setCurrentSemester(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学期不存在: " + id));
        semesterRepository.clearAllCurrentFlags();
        semester.setAsCurrent();
        semester = semesterRepository.save(semester);
        publishEvents(semester);
        return semester;
    }

    @Transactional
    public Semester endSemester(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学期不存在: " + id));
        semester.end();
        semester = semesterRepository.save(semester);
        publishEvents(semester);
        return semester;
    }

    @Transactional
    public Semester reactivateSemester(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学期不存在: " + id));
        semester.reactivate();
        return semesterRepository.save(semester);
    }

    public String generateSemesterCode(Integer startYear, Integer semesterType) {
        SemesterType type = SemesterType.fromCode(semesterType);
        return Semester.generateCode(startYear, type);
    }

    // ==================== 教学周管理 ====================

    @Transactional(readOnly = true)
    public List<TeachingWeek> getWeeks(Long semesterId) {
        return teachingWeekRepository.findBySemesterId(semesterId);
    }

    @Transactional
    public List<TeachingWeek> generateWeeks(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException("学期不存在: " + semesterId));

        // Delete existing weeks
        teachingWeekRepository.deleteBySemesterId(semesterId);

        // Generate weeks
        LocalDate weekStart = semester.getStartDate();
        LocalDate end = semester.getEndDate();
        int weekNum = 1;
        while (weekStart.isBefore(end)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(end)) weekEnd = end;
            TeachingWeek week = TeachingWeek.create(semesterId, weekNum,
                    "第" + weekNum + "周", weekStart, weekEnd);
            teachingWeekRepository.save(week);
            weekStart = weekEnd.plusDays(1);
            weekNum++;
        }

        return teachingWeekRepository.findBySemesterId(semesterId);
    }

    // ==================== 校历事件管理 ====================

    @Transactional(readOnly = true)
    public List<AcademicEvent> listEvents(Long yearId, Long semesterId, Integer eventType) {
        return academicEventRepository.findAll(yearId, semesterId, eventType);
    }

    @Transactional(readOnly = true)
    public Optional<AcademicEvent> getEvent(Long id) {
        return academicEventRepository.findById(id);
    }

    @Transactional
    public AcademicEvent createEvent(CreateAcademicEventCommand command) {
        AcademicEvent event = AcademicEvent.create(
                command.getYearId(), command.getSemesterId(), command.getEventName(),
                EventType.fromCode(command.getEventType()),
                command.getStartDate(), command.getEndDate(),
                command.getAllDay(), command.getDescription(),
                command.getAffectType(), command.getSubstituteWeekday(), command.getAffectSlots());
        return academicEventRepository.save(event);
    }

    @Transactional
    public AcademicEvent updateEvent(Long id, UpdateAcademicEventCommand command) {
        AcademicEvent event = academicEventRepository.findById(id)
                .orElseThrow(() -> new BusinessException("校历事件不存在: " + id));
        event.update(command.getEventName(), EventType.fromCode(command.getEventType()),
                command.getStartDate(), command.getEndDate(), command.getDescription(),
                command.getAffectType(), command.getSubstituteWeekday(), command.getAffectSlots());
        return academicEventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        academicEventRepository.deleteById(id);
    }

    // ==================== 校历网格 ====================

    @Transactional(readOnly = true)
    public CalendarGridDTO buildCalendarGrid(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException("学期不存在"));
        List<AcademicEvent> events = academicEventRepository.findAll(null, semesterId, null);

        Map<LocalDate, AcademicEvent> overrideMap = new LinkedHashMap<>();
        Map<LocalDate, AcademicEvent> infoMap = new LinkedHashMap<>();
        for (AcademicEvent e : events) {
            LocalDate d = e.getStartDate();
            LocalDate end = e.getEndDate() != null ? e.getEndDate() : d;
            if (e.getAffectType() != null && e.getAffectType() > 0) {
                while (!d.isAfter(end)) { overrideMap.put(d, e); d = d.plusDays(1); }
            } else {
                while (!d.isAfter(end)) { infoMap.put(d, e); d = d.plusDays(1); }
            }
        }

        List<CalendarWeekDTO> weeks = new ArrayList<>();
        LocalDate weekStart = semester.getStartDate();
        int weekNum = 1;
        int teachDays = 0, holidayDays = 0, makeupDays = 0, examDays = 0;

        while (!weekStart.isAfter(semester.getEndDate())) {
            List<CalendarDayDTO> days = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate date = weekStart.plusDays(i);
                if (date.isAfter(semester.getEndDate())) break;

                int weekday = date.getDayOfWeek().getValue();
                String dayType;
                String eventName = null;
                Integer followWeekday = null;

                AcademicEvent override = overrideMap.get(date);
                Long eventId = null;
                if (override != null) {
                    switch (override.getAffectType()) {
                        case 1: dayType = "HOLIDAY"; holidayDays++; break;
                        case 3: dayType = "MAKEUP"; makeupDays++;
                                followWeekday = override.getSubstituteWeekday(); break;
                        case 4: dayType = "EXAM"; examDays++; break;
                        default: dayType = weekday <= 5 ? "TEACHING" : "WEEKEND";
                    }
                    eventName = override.getEventName();
                    eventId = override.getId();
                } else {
                    dayType = weekday <= 5 ? "TEACHING" : "WEEKEND";
                    AcademicEvent info = infoMap.get(date);
                    if (info != null) {
                        eventName = info.getEventName();
                        eventId = info.getId();
                    }
                }
                if ("TEACHING".equals(dayType)) teachDays++;

                days.add(CalendarDayDTO.builder()
                        .date(date.toString()).weekday(weekday)
                        .dayType(dayType).eventName(eventName)
                        .followWeekday(followWeekday)
                        .eventId(eventId).build());
            }
            // compute weekType from weekday (Mon-Fri) day types
            String weekType = "TEACHING";
            long weekdayCount = days.stream().filter(d -> d.getWeekday() <= 5).count();
            if (weekdayCount > 0) {
                long holidays = days.stream().filter(d -> d.getWeekday() <= 5 && "HOLIDAY".equals(d.getDayType())).count();
                long exams = days.stream().filter(d -> d.getWeekday() <= 5 && "EXAM".equals(d.getDayType())).count();
                if (holidays == weekdayCount) weekType = "VACATION";
                else if (exams == weekdayCount) weekType = "EXAM";
            }

            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(semester.getEndDate())) weekEnd = semester.getEndDate();
            weeks.add(CalendarWeekDTO.builder()
                    .weekNumber(weekNum).startDate(weekStart.toString())
                    .endDate(weekEnd.toString()).weekType(weekType).days(days).build());
            weekStart = weekStart.plusDays(7);
            weekNum++;
        }

        return CalendarGridDTO.builder()
                .weeks(weeks)
                .totalTeachingDays(teachDays)
                .totalHolidayDays(holidayDays)
                .totalMakeupDays(makeupDays)
                .totalExamDays(examDays)
                .build();
    }

    // ==================== Helper ====================

    private void publishEvents(Semester semester) {
        semester.getDomainEvents().forEach(eventPublisher::publish);
        semester.clearDomainEvents();
    }
}
