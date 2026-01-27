package com.school.management.application.teaching;

import com.school.management.application.teaching.command.CreateGradeBatchCommand;
import com.school.management.application.teaching.command.RecordGradeCommand;
import com.school.management.application.teaching.query.GradeBatchDTO;
import com.school.management.application.teaching.query.StudentGradeDTO;
import com.school.management.domain.teaching.model.aggregate.StudentGrade;
import com.school.management.domain.teaching.model.entity.GradeItem;
import com.school.management.domain.teaching.repository.StudentGradeRepository;
import com.school.management.domain.teaching.service.GradeCalculationService;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeApplicationService {

    private final StudentGradeRepository gradeRepository;
    private final GradeCalculationService gradeCalculationService;

    @Transactional
    public Long createBatch(CreateGradeBatchCommand cmd) {
        StudentGrade batch = StudentGrade.builder()
                .semesterId(cmd.getSemesterId()).courseId(cmd.getCourseId()).classId(cmd.getClassId())
                .batchName(cmd.getBatchName()).gradeType(cmd.getGradeType())
                .status(0).createdBy(cmd.getOperatorId()).createdAt(LocalDateTime.now()).build();
        batch = gradeRepository.save(batch);
        log.info("创建成绩批次: id={}", batch.getId());
        return batch.getId();
    }

    @Transactional
    public void recordGrade(RecordGradeCommand cmd) {
        StudentGrade grade = gradeRepository.findByBatchIdAndStudentId(cmd.getBatchId(), cmd.getStudentId())
                .orElse(StudentGrade.builder().batchId(cmd.getBatchId()).studentId(cmd.getStudentId())
                        .status(0).createdAt(LocalDateTime.now()).build());

        grade.setTotalScore(cmd.getTotalScore());
        grade.setRemark(cmd.getRemark());
        grade.setStatus(1);
        grade.setUpdatedAt(LocalDateTime.now());

        // 计算等级和绩点
        if (cmd.getTotalScore() != null) {
            grade.setGradeLevel(gradeCalculationService.calculateLevel(cmd.getTotalScore()));
            grade.setGradePoint(gradeCalculationService.calculateGradePoint(cmd.getTotalScore()));
        }

        grade = gradeRepository.save(grade);

        // 保存分项成绩
        if (cmd.getItems() != null && !cmd.getItems().isEmpty()) {
            List<GradeItem> items = cmd.getItems().stream()
                    .map(i -> GradeItem.builder().gradeId(grade.getId())
                            .itemName(i.getItemName()).score(i.getScore()).build())
                    .collect(Collectors.toList());
            gradeRepository.saveGradeItems(grade.getId(), items);
        }

        log.info("录入成绩: gradeId={}, studentId={}", grade.getId(), cmd.getStudentId());
    }

    @Transactional
    public void batchRecordGrades(Long batchId, List<RecordGradeCommand> commands, Long operatorId) {
        for (RecordGradeCommand cmd : commands) {
            cmd.setBatchId(batchId);
            cmd.setOperatorId(operatorId);
            recordGrade(cmd);
        }
        log.info("批量录入成绩: batchId={}, count={}", batchId, commands.size());
    }

    @Transactional
    public void submitBatch(Long batchId, Long operatorId) {
        StudentGrade batch = gradeRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("批次不存在"));
        batch.setStatus(1);
        batch.setUpdatedBy(operatorId);
        batch.setUpdatedAt(LocalDateTime.now());
        gradeRepository.save(batch);
        log.info("提交成绩批次: id={}", batchId);
    }

    @Transactional
    public void publishBatch(Long batchId, Long operatorId) {
        StudentGrade batch = gradeRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("批次不存在"));
        batch.setStatus(3);
        batch.setUpdatedBy(operatorId);
        batch.setUpdatedAt(LocalDateTime.now());
        gradeRepository.save(batch);
        log.info("发布成绩批次: id={}", batchId);
    }

    public GradeBatchDTO getBatch(Long id) {
        return gradeRepository.findById(id).map(this::toBatchDTO)
                .orElseThrow(() -> new BusinessException("批次不存在"));
    }

    public List<GradeBatchDTO> getBatchesBySemester(Long semesterId) {
        return gradeRepository.findBySemesterId(semesterId).stream()
                .map(this::toBatchDTO).collect(Collectors.toList());
    }

    public List<StudentGradeDTO> getGradesByBatch(Long batchId) {
        return gradeRepository.findByBatchId(batchId).stream()
                .map(this::toGradeDTO).collect(Collectors.toList());
    }

    public List<StudentGradeDTO> getStudentGrades(Long studentId, Long semesterId) {
        return gradeRepository.findByStudentIdAndSemesterId(studentId, semesterId).stream()
                .map(this::toGradeDTO).collect(Collectors.toList());
    }

    public BigDecimal calculateGPA(Long studentId, Long semesterId) {
        List<StudentGrade> grades = gradeRepository.findByStudentIdAndSemesterId(studentId, semesterId);
        return gradeCalculationService.calculateGPA(grades);
    }

    public List<GradeBatchDTO> getBatchesPage(int page, int size, Long semesterId, Long courseId, Integer status) {
        return gradeRepository.findBatchesPage(page, size, semesterId, courseId, status).stream()
                .map(this::toBatchDTO).collect(Collectors.toList());
    }

    public long countBatches(Long semesterId, Long courseId, Integer status) {
        return gradeRepository.countBatches(semesterId, courseId, status);
    }

    private GradeBatchDTO toBatchDTO(StudentGrade g) {
        return GradeBatchDTO.builder().id(g.getId()).semesterId(g.getSemesterId()).courseId(g.getCourseId())
                .classId(g.getClassId()).batchName(g.getBatchName()).gradeType(g.getGradeType())
                .gradeTypeName(GradeBatchDTO.getGradeTypeName(g.getGradeType()))
                .status(g.getStatus()).statusName(GradeBatchDTO.getStatusName(g.getStatus()))
                .createdBy(g.getCreatedBy()).createdAt(g.getCreatedAt()).build();
    }

    private StudentGradeDTO toGradeDTO(StudentGrade g) {
        StudentGradeDTO dto = StudentGradeDTO.builder().id(g.getId()).batchId(g.getBatchId())
                .studentId(g.getStudentId()).courseId(g.getCourseId()).totalScore(g.getTotalScore())
                .gradeLevel(g.getGradeLevel()).gradePoint(g.getGradePoint())
                .status(g.getStatus()).statusName(StudentGradeDTO.getStatusName(g.getStatus()))
                .remark(g.getRemark()).createdAt(g.getCreatedAt()).updatedAt(g.getUpdatedAt()).build();

        if (g.getItems() != null) {
            dto.setItems(g.getItems().stream()
                    .map(i -> StudentGradeDTO.GradeItemDTO.builder()
                            .id(i.getId()).itemName(i.getItemName()).score(i.getScore()).weight(i.getWeight()).build())
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
