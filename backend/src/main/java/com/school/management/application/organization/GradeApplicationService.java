package com.school.management.application.organization;

import com.school.management.application.organization.command.AssignGradeLeaderCommand;
import com.school.management.application.organization.command.CreateGradeCommand;
import com.school.management.application.organization.command.UpdateGradeCommand;
import com.school.management.application.organization.query.GradeDTO;
import com.school.management.domain.organization.model.Grade;
import com.school.management.domain.organization.model.GradeStatus;
import com.school.management.domain.organization.repository.GradeRepository;
import com.school.management.domain.organization.repository.SchoolClassRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for grade operations.
 * Orchestrates domain services, repositories, and event publishing.
 */
@RequiredArgsConstructor
@Service
public class GradeApplicationService {

    private final GradeRepository gradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates a new grade.
     */
    @Transactional
    public GradeDTO createGrade(CreateGradeCommand command) {
        // 检查年级编码是否已存在
        if (gradeRepository.existsByGradeCode(command.getGradeCode())) {
            throw new IllegalArgumentException("年级编码已存在: " + command.getGradeCode());
        }

        // 检查入学年份是否已存在
        if (gradeRepository.existsByEnrollmentYear(command.getEnrollmentYear())) {
            throw new IllegalArgumentException("该入学年份已存在年级: " + command.getEnrollmentYear());
        }

        Grade grade = Grade.create(
            command.getGradeCode(),
            command.getGradeName(),
            command.getEnrollmentYear(),
            command.getSchoolingYears(),
            command.getCreatedBy()
        );

        grade = gradeRepository.save(grade);

        // Publish domain events
        grade.getDomainEvents().forEach(eventPublisher::publish);
        grade.clearDomainEvents();

        return toDTO(grade);
    }

    /**
     * Updates an existing grade.
     */
    @Transactional
    public GradeDTO updateGrade(Long id, UpdateGradeCommand command) {
        Grade grade = gradeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        grade.updateInfo(
            command.getGradeName(),
            command.getStandardClassSize(),
            command.getSortOrder(),
            command.getRemarks(),
            command.getUpdatedBy()
        );

        grade = gradeRepository.save(grade);

        // Publish domain events
        grade.getDomainEvents().forEach(eventPublisher::publish);
        grade.clearDomainEvents();

        return toDTO(grade);
    }

    /**
     * Assigns director and counselor to a grade.
     */
    @Transactional
    public GradeDTO assignLeaders(Long id, AssignGradeLeaderCommand command) {
        Grade grade = gradeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        if (command.getDirectorId() != null) {
            grade.assignDirector(command.getDirectorId(), command.getDirectorName(), command.getUpdatedBy());
        }

        if (command.getCounselorId() != null) {
            grade.assignCounselor(command.getCounselorId(), command.getCounselorName(), command.getUpdatedBy());
        }

        grade = gradeRepository.save(grade);

        return toDTO(grade);
    }

    /**
     * Gets a grade by ID.
     */
    @Transactional(readOnly = true)
    public GradeDTO getGrade(Long id) {
        return gradeRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));
    }

    /**
     * Gets a grade by enrollment year.
     */
    @Transactional(readOnly = true)
    public GradeDTO getGradeByEnrollmentYear(Integer enrollmentYear) {
        return gradeRepository.findByEnrollmentYear(enrollmentYear)
            .map(this::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + enrollmentYear + "级"));
    }

    /**
     * Gets all grades.
     */
    @Transactional(readOnly = true)
    public List<GradeDTO> getAllGrades() {
        return gradeRepository.findAllOrderByEnrollmentYearDesc().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets grades by status.
     */
    @Transactional(readOnly = true)
    public List<GradeDTO> getGradesByStatus(GradeStatus status) {
        return gradeRepository.findByStatus(status).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets active grades.
     */
    @Transactional(readOnly = true)
    public List<GradeDTO> getActiveGrades() {
        return gradeRepository.findActiveGrades().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Activates a grade.
     */
    @Transactional
    public GradeDTO activateGrade(Long id, Long updatedBy) {
        Grade grade = gradeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        grade.activate(updatedBy);
        grade = gradeRepository.save(grade);

        // Publish domain events
        grade.getDomainEvents().forEach(eventPublisher::publish);
        grade.clearDomainEvents();

        return toDTO(grade);
    }

    /**
     * Graduates a grade.
     */
    @Transactional
    public GradeDTO graduateGrade(Long id, Long updatedBy) {
        Grade grade = gradeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        grade.graduate(updatedBy);
        grade = gradeRepository.save(grade);

        // Publish domain events
        grade.getDomainEvents().forEach(eventPublisher::publish);
        grade.clearDomainEvents();

        return toDTO(grade);
    }

    /**
     * Stops enrollment for a grade.
     */
    @Transactional
    public GradeDTO stopEnrollment(Long id, Long updatedBy) {
        Grade grade = gradeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        grade.stopEnrollment(updatedBy);
        grade = gradeRepository.save(grade);

        // Publish domain events
        grade.getDomainEvents().forEach(eventPublisher::publish);
        grade.clearDomainEvents();

        return toDTO(grade);
    }

    /**
     * Deletes a grade.
     */
    @Transactional
    public void deleteGrade(Long id) {
        Grade grade = gradeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        // 检查年级下是否有班级
        int classCount = schoolClassRepository.countByGradeId(id);
        if (classCount > 0) {
            throw new IllegalStateException(
                String.format("无法删除年级: 该年级下还有 %d 个班级", classCount));
        }

        gradeRepository.deleteById(id);
    }

    // Helper methods
    private GradeDTO toDTO(Grade grade) {
        GradeDTO dto = new GradeDTO();
        dto.setId(grade.getId());
        dto.setGradeCode(grade.getGradeCode());
        dto.setGradeName(grade.getGradeName());
        dto.setEnrollmentYear(grade.getEnrollmentYear());
        dto.setGraduationYear(grade.getGraduationYear());
        dto.setSchoolingYears(grade.getSchoolingYears());
        dto.setDirectorId(grade.getDirectorId());
        dto.setDirectorName(grade.getDirectorName());
        dto.setCounselorId(grade.getCounselorId());
        dto.setCounselorName(grade.getCounselorName());
        dto.setStandardClassSize(grade.getStandardClassSize());
        dto.setStatus(grade.getStatus());
        dto.setStatusDisplayName(grade.getStatus() != null ? grade.getStatus().getDisplayName() : null);
        dto.setSortOrder(grade.getSortOrder());
        dto.setRemarks(grade.getRemarks());
        dto.setCreatedAt(grade.getCreatedAt());
        dto.setUpdatedAt(grade.getUpdatedAt());
        return dto;
    }
}
