package com.school.management.application.teaching;

import com.school.management.application.teaching.command.CreateExamArrangementCommand;
import com.school.management.application.teaching.command.CreateExamBatchCommand;
import com.school.management.application.teaching.query.ExamArrangementDTO;
import com.school.management.application.teaching.query.ExamBatchDTO;
import com.school.management.application.teaching.query.ExamRoomDTO;
import com.school.management.domain.teaching.model.aggregate.Examination;
import com.school.management.domain.teaching.model.entity.ExamArrangement;
import com.school.management.domain.teaching.model.entity.ExamInvigilator;
import com.school.management.domain.teaching.model.entity.ExamRoom;
import com.school.management.domain.teaching.repository.ExaminationRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 考试管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExaminationApplicationService {

    private final ExaminationRepository examinationRepository;

    /**
     * 创建考试批次
     */
    @Transactional
    public Long createBatch(CreateExamBatchCommand command) {
        // 验证日期
        if (command.getEndDate().isBefore(command.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }

        Examination batch = Examination.builder()
                .semesterId(command.getSemesterId())
                .batchName(command.getBatchName())
                .examType(command.getExamType())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .status(0) // 草稿
                .remark(command.getRemark())
                .createdBy(command.getOperatorId())
                .createdAt(LocalDateTime.now())
                .build();

        batch = examinationRepository.save(batch);
        log.info("创建考试批次成功: id={}, name={}", batch.getId(), batch.getBatchName());
        return batch.getId();
    }

    /**
     * 更新考试批次
     */
    @Transactional
    public void updateBatch(Long id, String batchName, Integer examType,
                            java.time.LocalDate startDate, java.time.LocalDate endDate,
                            String remark, Long operatorId) {
        Examination batch = examinationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("考试批次不存在"));

        if (batch.getStatus() >= 2) {
            throw new BusinessException("进行中或已结束的批次不能修改");
        }

        if (batchName != null) batch.setBatchName(batchName);
        if (examType != null) batch.setExamType(examType);
        if (startDate != null) batch.setStartDate(startDate);
        if (endDate != null) batch.setEndDate(endDate);
        if (remark != null) batch.setRemark(remark);

        batch.setUpdatedBy(operatorId);
        batch.setUpdatedAt(LocalDateTime.now());

        examinationRepository.save(batch);
        log.info("更新考试批次成功: id={}", id);
    }

    /**
     * 发布考试批次
     */
    @Transactional
    public void publishBatch(Long id, Long operatorId) {
        Examination batch = examinationRepository.findByIdWithArrangements(id)
                .orElseThrow(() -> new BusinessException("考试批次不存在"));

        if (batch.getStatus() != 0) {
            throw new BusinessException("只能发布草稿状态的批次");
        }

        if (batch.getArrangements() == null || batch.getArrangements().isEmpty()) {
            throw new BusinessException("请先添加考试安排");
        }

        batch.setStatus(1);
        batch.setUpdatedBy(operatorId);
        batch.setUpdatedAt(LocalDateTime.now());
        examinationRepository.save(batch);

        log.info("发布考试批次成功: id={}", id);
    }

    /**
     * 删除考试批次
     */
    @Transactional
    public void deleteBatch(Long id) {
        Examination batch = examinationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("考试批次不存在"));

        if (batch.getStatus() >= 2) {
            throw new BusinessException("进行中或已结束的批次不能删除");
        }

        examinationRepository.deleteById(id);
        log.info("删除考试批次成功: id={}", id);
    }

    /**
     * 添加考试安排
     */
    @Transactional
    public Long addArrangement(CreateExamArrangementCommand command) {
        Examination batch = examinationRepository.findById(command.getBatchId())
                .orElseThrow(() -> new BusinessException("考试批次不存在"));

        if (batch.getStatus() >= 2) {
            throw new BusinessException("进行中或已结束的批次不能添加安排");
        }

        // 验证考试日期在批次范围内
        if (command.getExamDate().isBefore(batch.getStartDate()) ||
                command.getExamDate().isAfter(batch.getEndDate())) {
            throw new BusinessException("考试日期必须在批次日期范围内");
        }

        ExamArrangement arrangement = ExamArrangement.builder()
                .batchId(command.getBatchId())
                .courseId(command.getCourseId())
                .examDate(command.getExamDate())
                .startTime(command.getStartTime())
                .endTime(command.getEndTime())
                .status(0) // 待考
                .remark(command.getRemark())
                .createdAt(LocalDateTime.now())
                .build();

        arrangement = examinationRepository.saveArrangement(arrangement);

        // 保存考试教室
        if (command.getExamRooms() != null && !command.getExamRooms().isEmpty()) {
            List<ExamRoom> rooms = new ArrayList<>();
            for (CreateExamArrangementCommand.ExamRoomItem item : command.getExamRooms()) {
                ExamRoom room = ExamRoom.builder()
                        .arrangementId(arrangement.getId())
                        .classroomId(item.getClassroomId())
                        .capacity(item.getCapacity())
                        .build();
                rooms.add(room);
            }
            examinationRepository.saveExamRooms(arrangement.getId(), rooms);

            // 保存监考教师
            for (int i = 0; i < command.getExamRooms().size(); i++) {
                CreateExamArrangementCommand.ExamRoomItem item = command.getExamRooms().get(i);
                if (item.getInvigilatorIds() != null && !item.getInvigilatorIds().isEmpty()) {
                    ExamRoom room = rooms.get(i);
                    List<ExamInvigilator> invigilators = new ArrayList<>();
                    boolean first = true;
                    for (Long teacherId : item.getInvigilatorIds()) {
                        ExamInvigilator inv = ExamInvigilator.builder()
                                .roomId(room.getId())
                                .teacherId(teacherId)
                                .isMain(first) // 第一个为主监考
                                .build();
                        invigilators.add(inv);
                        first = false;
                    }
                    examinationRepository.saveInvigilators(room.getId(), invigilators);
                }
            }
        }

        log.info("添加考试安排成功: id={}, courseId={}", arrangement.getId(), command.getCourseId());
        return arrangement.getId();
    }

    /**
     * 删除考试安排
     */
    @Transactional
    public void deleteArrangement(Long arrangementId) {
        ExamArrangement arrangement = examinationRepository.findArrangementById(arrangementId)
                .orElseThrow(() -> new BusinessException("考试安排不存在"));

        if (arrangement.getStatus() >= 1) {
            throw new BusinessException("进行中或已完成的安排不能删除");
        }

        examinationRepository.deleteArrangement(arrangementId);
        log.info("删除考试安排成功: id={}", arrangementId);
    }

    /**
     * 获取考试批次详情
     */
    public ExamBatchDTO getBatch(Long id) {
        return examinationRepository.findByIdWithArrangements(id)
                .map(this::toBatchDTO)
                .orElseThrow(() -> new BusinessException("考试批次不存在"));
    }

    /**
     * 根据学期获取考试批次列表
     */
    public List<ExamBatchDTO> getBatchesBySemester(Long semesterId) {
        return examinationRepository.findBySemesterId(semesterId).stream()
                .map(this::toBatchDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取考试安排详情
     */
    public ExamArrangementDTO getArrangement(Long id) {
        return examinationRepository.findArrangementByIdWithRooms(id)
                .map(this::toArrangementDTO)
                .orElseThrow(() -> new BusinessException("考试安排不存在"));
    }

    /**
     * 根据课程获取考试安排
     */
    public List<ExamArrangementDTO> getArrangementsByCourse(Long semesterId, Long courseId) {
        return examinationRepository.findArrangementsByCourseId(semesterId, courseId).stream()
                .map(this::toArrangementDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据教室获取考试安排
     */
    public List<ExamArrangementDTO> getArrangementsByClassroom(Long semesterId, Long classroomId) {
        return examinationRepository.findArrangementsByClassroomId(semesterId, classroomId).stream()
                .map(this::toArrangementDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询考试批次
     */
    public List<ExamBatchDTO> getBatchesPage(int page, int size, Long semesterId,
                                              Integer examType, Integer status) {
        return examinationRepository.findBatchesPage(page, size, semesterId, examType, status).stream()
                .map(this::toBatchDTO)
                .collect(Collectors.toList());
    }

    /**
     * 统计考试批次数量
     */
    public long countBatches(Long semesterId, Integer examType, Integer status) {
        return examinationRepository.countBatches(semesterId, examType, status);
    }

    private ExamBatchDTO toBatchDTO(Examination exam) {
        ExamBatchDTO dto = ExamBatchDTO.builder()
                .id(exam.getId())
                .semesterId(exam.getSemesterId())
                .batchName(exam.getBatchName())
                .examType(exam.getExamType())
                .examTypeName(ExamBatchDTO.getExamTypeName(exam.getExamType()))
                .startDate(exam.getStartDate())
                .endDate(exam.getEndDate())
                .status(exam.getStatus())
                .statusName(ExamBatchDTO.getStatusName(exam.getStatus()))
                .remark(exam.getRemark())
                .createdBy(exam.getCreatedBy())
                .createdAt(exam.getCreatedAt())
                .build();

        if (exam.getArrangements() != null) {
            dto.setArrangements(exam.getArrangements().stream()
                    .map(this::toArrangementDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private ExamArrangementDTO toArrangementDTO(ExamArrangement arrangement) {
        ExamArrangementDTO dto = ExamArrangementDTO.builder()
                .id(arrangement.getId())
                .batchId(arrangement.getBatchId())
                .courseId(arrangement.getCourseId())
                .examDate(arrangement.getExamDate())
                .weekday(arrangement.getExamDate() != null ?
                        arrangement.getExamDate().getDayOfWeek().getValue() : null)
                .startTime(arrangement.getStartTime())
                .endTime(arrangement.getEndTime())
                .status(arrangement.getStatus())
                .statusName(ExamArrangementDTO.getStatusName(arrangement.getStatus()))
                .remark(arrangement.getRemark())
                .build();

        if (dto.getWeekday() != null) {
            dto.setWeekdayName(ScheduleEntryDTO.getWeekdayName(dto.getWeekday()));
        }

        if (arrangement.getStartTime() != null && arrangement.getEndTime() != null) {
            dto.setTimeRange(arrangement.getStartTime() + " - " + arrangement.getEndTime());
        }

        if (arrangement.getExamRooms() != null) {
            dto.setExamRooms(arrangement.getExamRooms().stream()
                    .map(this::toRoomDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private ExamRoomDTO toRoomDTO(ExamRoom room) {
        ExamRoomDTO dto = ExamRoomDTO.builder()
                .id(room.getId())
                .arrangementId(room.getArrangementId())
                .classroomId(room.getClassroomId())
                .capacity(room.getCapacity())
                .actualCount(room.getActualCount())
                .build();

        if (room.getInvigilators() != null) {
            dto.setInvigilators(room.getInvigilators().stream()
                    .map(inv -> ExamRoomDTO.InvigilatorDTO.builder()
                            .id(inv.getId())
                            .teacherId(inv.getTeacherId())
                            .isMain(inv.getIsMain())
                            .build())
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
