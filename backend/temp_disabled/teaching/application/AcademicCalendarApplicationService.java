package com.school.management.application.teaching;

import com.school.management.application.teaching.command.*;
import com.school.management.application.teaching.query.*;
import com.school.management.domain.teaching.model.aggregate.AcademicYear;
import com.school.management.domain.teaching.model.aggregate.Semester;
import com.school.management.domain.teaching.model.entity.SchoolEvent;
import com.school.management.domain.teaching.model.entity.TeachingWeek;
import com.school.management.domain.teaching.repository.AcademicYearRepository;
import com.school.management.domain.teaching.repository.SemesterRepository;
import com.school.management.domain.teaching.repository.SchoolEventRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 校历管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicCalendarApplicationService {

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final SchoolEventRepository schoolEventRepository;

    // ==================== 学年管理 ====================

    /**
     * 创建学年
     */
    @Transactional
    public Long createAcademicYear(CreateAcademicYearCommand command) {
        log.info("创建学年: {}", command.getYearCode());

        // 检查学年代码是否已存在
        if (academicYearRepository.existsByYearCode(command.getYearCode())) {
            throw new BusinessException("学年代码已存在: " + command.getYearCode());
        }

        AcademicYear academicYear = AcademicYear.builder()
                .yearCode(command.getYearCode())
                .yearName(command.getYearName())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .isCurrent(false)
                .status(1)
                .createdBy(command.getOperatorId())
                .createdAt(LocalDateTime.now())
                .build();

        AcademicYear saved = academicYearRepository.save(academicYear);
        log.info("学年创建成功: id={}", saved.getId());
        return saved.getId();
    }

    /**
     * 更新学年
     */
    @Transactional
    public void updateAcademicYear(UpdateAcademicYearCommand command) {
        log.info("更新学年: id={}", command.getId());

        AcademicYear academicYear = academicYearRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessException("学年不存在: " + command.getId()));

        // 检查学年代码是否冲突
        if (!academicYear.getYearCode().equals(command.getYearCode())
                && academicYearRepository.existsByYearCode(command.getYearCode())) {
            throw new BusinessException("学年代码已存在: " + command.getYearCode());
        }

        academicYear.setYearCode(command.getYearCode());
        academicYear.setYearName(command.getYearName());
        academicYear.setStartDate(command.getStartDate());
        academicYear.setEndDate(command.getEndDate());
        academicYear.setUpdatedBy(command.getOperatorId());
        academicYear.setUpdatedAt(LocalDateTime.now());

        academicYearRepository.save(academicYear);
        log.info("学年更新成功: id={}", command.getId());
    }

    /**
     * 设为当前学年
     */
    @Transactional
    public void setCurrentAcademicYear(Long id) {
        log.info("设为当前学年: id={}", id);

        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学年不存在: " + id));

        // 清除所有当前学年标记
        academicYearRepository.clearAllCurrent();

        // 设为当前学年
        academicYear.setAsCurrent();
        academicYearRepository.save(academicYear);

        log.info("设为当前学年成功: id={}", id);
    }

    /**
     * 删除学年
     */
    @Transactional
    public void deleteAcademicYear(Long id) {
        log.info("删除学年: id={}", id);

        // 检查是否有关联学期
        List<Semester> semesters = semesterRepository.findByAcademicYearId(id);
        if (!semesters.isEmpty()) {
            throw new BusinessException("该学年下存在学期，无法删除");
        }

        academicYearRepository.deleteById(id);
        log.info("学年删除成功: id={}", id);
    }

    /**
     * 查询学年详情
     */
    public AcademicYearDTO getAcademicYear(Long id) {
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学年不存在: " + id));
        return toAcademicYearDTO(academicYear);
    }

    /**
     * 查询所有学年
     */
    public List<AcademicYearDTO> listAcademicYears() {
        return academicYearRepository.findAll().stream()
                .map(this::toAcademicYearDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询当前学年
     */
    public AcademicYearDTO getCurrentAcademicYear() {
        return academicYearRepository.findCurrent()
                .map(this::toAcademicYearDTO)
                .orElse(null);
    }

    // ==================== 学期管理 ====================

    /**
     * 创建学期
     */
    @Transactional
    public Long createSemester(CreateSemesterCommand command) {
        log.info("创建学期: {}", command.getSemesterCode());

        // 验证学年存在
        academicYearRepository.findById(command.getAcademicYearId())
                .orElseThrow(() -> new BusinessException("学年不存在: " + command.getAcademicYearId()));

        // 检查学期代码是否已存在
        if (semesterRepository.existsBySemesterCode(command.getSemesterCode())) {
            throw new BusinessException("学期代码已存在: " + command.getSemesterCode());
        }

        Semester semester = Semester.builder()
                .academicYearId(command.getAcademicYearId())
                .semesterCode(command.getSemesterCode())
                .semesterName(command.getSemesterName())
                .semesterType(command.getSemesterType())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .teachingStartDate(command.getTeachingStartDate())
                .teachingEndDate(command.getTeachingEndDate())
                .examStartDate(command.getExamStartDate())
                .examEndDate(command.getExamEndDate())
                .totalTeachingWeeks(command.getTotalTeachingWeeks())
                .isCurrent(false)
                .status(1)
                .createdBy(command.getOperatorId())
                .createdAt(LocalDateTime.now())
                .build();

        Semester saved = semesterRepository.save(semester);
        log.info("学期创建成功: id={}", saved.getId());
        return saved.getId();
    }

    /**
     * 更新学期
     */
    @Transactional
    public void updateSemester(UpdateSemesterCommand command) {
        log.info("更新学期: id={}", command.getId());

        Semester semester = semesterRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessException("学期不存在: " + command.getId()));

        semester.setSemesterName(command.getSemesterName());
        semester.setStartDate(command.getStartDate());
        semester.setEndDate(command.getEndDate());
        semester.setTeachingStartDate(command.getTeachingStartDate());
        semester.setTeachingEndDate(command.getTeachingEndDate());
        semester.setExamStartDate(command.getExamStartDate());
        semester.setExamEndDate(command.getExamEndDate());
        semester.setTotalTeachingWeeks(command.getTotalTeachingWeeks());
        semester.setUpdatedBy(command.getOperatorId());
        semester.setUpdatedAt(LocalDateTime.now());

        semesterRepository.save(semester);
        log.info("学期更新成功: id={}", command.getId());
    }

    /**
     * 设为当前学期
     */
    @Transactional
    public void setCurrentSemester(Long id) {
        log.info("设为当前学期: id={}", id);

        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学期不存在: " + id));

        // 清除所有当前学期标记
        semesterRepository.clearAllCurrent();

        // 设为当前学期
        semester.setAsCurrent();
        semesterRepository.save(semester);

        log.info("设为当前学期成功: id={}", id);
    }

    /**
     * 删除学期
     */
    @Transactional
    public void deleteSemester(Long id) {
        log.info("删除学期: id={}", id);
        semesterRepository.deleteById(id);
        log.info("学期删除成功: id={}", id);
    }

    /**
     * 查询学期详情
     */
    public SemesterDTO getSemester(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学期不存在: " + id));
        return toSemesterDTO(semester);
    }

    /**
     * 查询学年下的所有学期
     */
    public List<SemesterDTO> listSemestersByAcademicYear(Long academicYearId) {
        return semesterRepository.findByAcademicYearId(academicYearId).stream()
                .map(this::toSemesterDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询所有学期
     */
    public List<SemesterDTO> listAllSemesters() {
        return semesterRepository.findAll().stream()
                .map(this::toSemesterDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询当前学期
     */
    public SemesterDTO getCurrentSemester() {
        return semesterRepository.findCurrent()
                .map(this::toSemesterDTO)
                .orElse(null);
    }

    // ==================== 教学周管理 ====================

    /**
     * 自动生成教学周
     */
    @Transactional
    public List<TeachingWeekDTO> generateTeachingWeeks(Long semesterId) {
        log.info("自动生成教学周: semesterId={}", semesterId);

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException("学期不存在: " + semesterId));

        List<TeachingWeek> weeks = semester.generateTeachingWeeks();
        semesterRepository.saveTeachingWeeks(semesterId, weeks);

        log.info("教学周生成成功: {} 周", weeks.size());
        return weeks.stream().map(this::toTeachingWeekDTO).collect(Collectors.toList());
    }

    /**
     * 查询学期的教学周
     */
    public List<TeachingWeekDTO> listTeachingWeeks(Long semesterId) {
        Semester semester = semesterRepository.findByIdWithWeeks(semesterId)
                .orElseThrow(() -> new BusinessException("学期不存在: " + semesterId));
        return semester.getTeachingWeeks().stream()
                .map(this::toTeachingWeekDTO)
                .collect(Collectors.toList());
    }

    // ==================== 校历事件管理 ====================

    /**
     * 创建校历事件
     */
    @Transactional
    public Long createSchoolEvent(CreateSchoolEventCommand command) {
        log.info("创建校历事件: {}", command.getEventName());

        SchoolEvent event = SchoolEvent.builder()
                .semesterId(command.getSemesterId())
                .eventCode(command.getEventCode())
                .eventName(command.getEventName())
                .eventType(command.getEventType())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .startTime(command.getStartTime())
                .endTime(command.getEndTime())
                .allDay(command.getAllDay())
                .affectSchedule(command.getAffectSchedule())
                .affectedOrgUnits(command.getAffectedOrgUnits())
                .swapToDate(command.getSwapToDate())
                .swapWeekday(command.getSwapWeekday())
                .color(command.getColor())
                .description(command.getDescription())
                .status(1)
                .createdBy(command.getOperatorId())
                .createdAt(LocalDateTime.now())
                .build();

        SchoolEvent saved = schoolEventRepository.save(event);
        log.info("校历事件创建成功: id={}", saved.getId());
        return saved.getId();
    }

    /**
     * 更新校历事件
     */
    @Transactional
    public void updateSchoolEvent(UpdateSchoolEventCommand command) {
        log.info("更新校历事件: id={}", command.getId());

        SchoolEvent event = schoolEventRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessException("校历事件不存在: " + command.getId()));

        event.setEventName(command.getEventName());
        event.setEventType(command.getEventType());
        event.setStartDate(command.getStartDate());
        event.setEndDate(command.getEndDate());
        event.setStartTime(command.getStartTime());
        event.setEndTime(command.getEndTime());
        event.setAllDay(command.getAllDay());
        event.setAffectSchedule(command.getAffectSchedule());
        event.setAffectedOrgUnits(command.getAffectedOrgUnits());
        event.setSwapToDate(command.getSwapToDate());
        event.setSwapWeekday(command.getSwapWeekday());
        event.setColor(command.getColor());
        event.setDescription(command.getDescription());
        event.setUpdatedBy(command.getOperatorId());
        event.setUpdatedAt(LocalDateTime.now());

        schoolEventRepository.save(event);
        log.info("校历事件更新成功: id={}", command.getId());
    }

    /**
     * 删除校历事件
     */
    @Transactional
    public void deleteSchoolEvent(Long id) {
        log.info("删除校历事件: id={}", id);
        schoolEventRepository.deleteById(id);
        log.info("校历事件删除成功: id={}", id);
    }

    /**
     * 查询校历事件
     */
    public SchoolEventDTO getSchoolEvent(Long id) {
        SchoolEvent event = schoolEventRepository.findById(id)
                .orElseThrow(() -> new BusinessException("校历事件不存在: " + id));
        return toSchoolEventDTO(event);
    }

    /**
     * 查询日期范围内的事件
     */
    public List<SchoolEventDTO> listEventsByDateRange(LocalDate startDate, LocalDate endDate) {
        return schoolEventRepository.findByDateRange(startDate, endDate).stream()
                .map(this::toSchoolEventDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询学期的所有事件
     */
    public List<SchoolEventDTO> listEventsBySemester(Long semesterId) {
        return schoolEventRepository.findBySemesterId(semesterId).stream()
                .map(this::toSchoolEventDTO)
                .collect(Collectors.toList());
    }

    // ==================== DTO转换 ====================

    private AcademicYearDTO toAcademicYearDTO(AcademicYear ay) {
        return AcademicYearDTO.builder()
                .id(ay.getId())
                .yearCode(ay.getYearCode())
                .yearName(ay.getYearName())
                .startDate(ay.getStartDate())
                .endDate(ay.getEndDate())
                .isCurrent(ay.getIsCurrent())
                .status(ay.getStatus())
                .createdAt(ay.getCreatedAt())
                .build();
    }

    private SemesterDTO toSemesterDTO(Semester s) {
        return SemesterDTO.builder()
                .id(s.getId())
                .academicYearId(s.getAcademicYearId())
                .semesterCode(s.getSemesterCode())
                .semesterName(s.getSemesterName())
                .semesterType(s.getSemesterType())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .teachingStartDate(s.getTeachingStartDate())
                .teachingEndDate(s.getTeachingEndDate())
                .examStartDate(s.getExamStartDate())
                .examEndDate(s.getExamEndDate())
                .totalTeachingWeeks(s.getTotalTeachingWeeks())
                .isCurrent(s.getIsCurrent())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private TeachingWeekDTO toTeachingWeekDTO(TeachingWeek w) {
        return TeachingWeekDTO.builder()
                .id(w.getId())
                .semesterId(w.getSemesterId())
                .weekNumber(w.getWeekNumber())
                .startDate(w.getStartDate())
                .endDate(w.getEndDate())
                .weekType(w.getWeekType())
                .weekTypeName(w.getWeekTypeName())
                .weekLabel(w.getWeekLabel())
                .isActive(w.getIsActive())
                .remark(w.getRemark())
                .build();
    }

    private SchoolEventDTO toSchoolEventDTO(SchoolEvent e) {
        return SchoolEventDTO.builder()
                .id(e.getId())
                .semesterId(e.getSemesterId())
                .eventCode(e.getEventCode())
                .eventName(e.getEventName())
                .eventType(e.getEventType())
                .eventTypeName(e.getEventTypeName())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .allDay(e.getAllDay())
                .affectSchedule(e.getAffectSchedule())
                .swapToDate(e.getSwapToDate())
                .swapWeekday(e.getSwapWeekday())
                .color(e.getColor())
                .description(e.getDescription())
                .status(e.getStatus())
                .build();
    }
}
