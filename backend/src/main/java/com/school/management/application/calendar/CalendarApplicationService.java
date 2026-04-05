package com.school.management.application.calendar;

import com.school.management.application.calendar.command.*;
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
import java.util.List;
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
        return academicYearRepository.findById(yearId)
                .map(year -> semesterRepository.findByDateRange(year.getStartDate(), year.getEndDate()))
                .orElseGet(this::listSemesters);
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
        SemesterType semesterType = command.getSemesterType() != null
                ? SemesterType.fromCode(command.getSemesterType()) : SemesterType.FIRST;
        Integer startYear = command.getStartDate().getYear();
        String semesterCode = command.getSemesterCode() != null
                ? command.getSemesterCode()
                : Semester.generateCode(startYear, semesterType);

        if (semesterRepository.existsBySemesterCode(semesterCode)) {
            throw new BusinessException("学期编码已存在: " + semesterCode);
        }

        Semester semester = Semester.create(command.getSemesterName(), semesterCode,
                command.getStartDate(), command.getEndDate(), startYear, semesterType);
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
                command.getAllDay(), command.getDescription());
        return academicEventRepository.save(event);
    }

    @Transactional
    public AcademicEvent updateEvent(Long id, UpdateAcademicEventCommand command) {
        AcademicEvent event = academicEventRepository.findById(id)
                .orElseThrow(() -> new BusinessException("校历事件不存在: " + id));
        event.update(command.getEventName(), EventType.fromCode(command.getEventType()),
                command.getStartDate(), command.getEndDate(), command.getDescription());
        return academicEventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        academicEventRepository.deleteById(id);
    }

    // ==================== Helper ====================

    private void publishEvents(Semester semester) {
        semester.getDomainEvents().forEach(eventPublisher::publish);
        semester.clearDomainEvents();
    }
}
