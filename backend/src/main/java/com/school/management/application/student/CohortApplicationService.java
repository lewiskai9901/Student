package com.school.management.application.student;

import com.school.management.application.student.command.AssignCohortLeaderCommand;
import com.school.management.application.student.command.CreateCohortCommand;
import com.school.management.application.student.command.UpdateCohortCommand;
import com.school.management.application.student.query.CohortDTO;
import com.school.management.domain.student.model.Cohort;
import com.school.management.domain.student.model.CohortStatus;
import com.school.management.domain.student.repository.CohortRepository;
import com.school.management.domain.student.repository.SchoolClassRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for cohort operations.
 * Orchestrates domain services, repositories, and event publishing.
 */
@RequiredArgsConstructor
@Service
public class CohortApplicationService {

    private final CohortRepository cohortRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates a new cohort.
     */
    @Transactional
    public CohortDTO createCohort(CreateCohortCommand command) {
        // 检查年级编码是否已存在
        if (cohortRepository.existsByGradeCode(command.getGradeCode())) {
            throw new IllegalArgumentException("年级编码已存在: " + command.getGradeCode());
        }

        // 检查入学年份是否已存在
        if (cohortRepository.existsByEnrollmentYear(command.getEnrollmentYear())) {
            throw new IllegalArgumentException("该入学年份已存在年级: " + command.getEnrollmentYear());
        }

        Cohort cohort = Cohort.create(
            command.getGradeCode(),
            command.getGradeName(),
            command.getEnrollmentYear(),
            command.getSchoolingYears(),
            command.getCreatedBy()
        );

        cohort = cohortRepository.save(cohort);

        // Publish domain events
        cohort.getDomainEvents().forEach(eventPublisher::publish);
        cohort.clearDomainEvents();

        return toDTO(cohort);
    }

    /**
     * Updates an existing cohort.
     */
    @Transactional
    public CohortDTO updateCohort(Long id, UpdateCohortCommand command) {
        Cohort cohort = cohortRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        cohort.updateInfo(
            command.getGradeName(),
            command.getStandardClassSize(),
            command.getSortOrder(),
            command.getRemarks(),
            command.getUpdatedBy()
        );

        cohort = cohortRepository.save(cohort);

        // Publish domain events
        cohort.getDomainEvents().forEach(eventPublisher::publish);
        cohort.clearDomainEvents();

        return toDTO(cohort);
    }

    /**
     * Assigns director and counselor to a cohort.
     */
    @Transactional
    public CohortDTO assignLeaders(Long id, AssignCohortLeaderCommand command) {
        Cohort cohort = cohortRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        if (command.getDirectorId() != null) {
            cohort.assignDirector(command.getDirectorId(), command.getDirectorName(), command.getUpdatedBy());
        }

        if (command.getCounselorId() != null) {
            cohort.assignCounselor(command.getCounselorId(), command.getCounselorName(), command.getUpdatedBy());
        }

        cohort = cohortRepository.save(cohort);

        return toDTO(cohort);
    }

    /**
     * Gets a cohort by ID.
     */
    @Transactional(readOnly = true)
    public CohortDTO getCohort(Long id) {
        return cohortRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));
    }

    /**
     * Gets a cohort by enrollment year.
     */
    @Transactional(readOnly = true)
    public CohortDTO getCohortByEnrollmentYear(Integer enrollmentYear) {
        return cohortRepository.findByEnrollmentYear(enrollmentYear)
            .map(this::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + enrollmentYear + "级"));
    }

    /**
     * Gets all cohorts.
     */
    @Transactional(readOnly = true)
    public List<CohortDTO> getAllCohorts() {
        return cohortRepository.findAllOrderByEnrollmentYearDesc().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets cohorts by status.
     */
    @Transactional(readOnly = true)
    public List<CohortDTO> getCohortsByStatus(CohortStatus status) {
        return cohortRepository.findByStatus(status).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets active cohorts.
     */
    @Transactional(readOnly = true)
    public List<CohortDTO> getActiveCohorts() {
        return cohortRepository.findActiveCohorts().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Activates a cohort.
     */
    @Transactional
    public CohortDTO activateCohort(Long id, Long updatedBy) {
        Cohort cohort = cohortRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        cohort.activate(updatedBy);
        cohort = cohortRepository.save(cohort);

        // Publish domain events
        cohort.getDomainEvents().forEach(eventPublisher::publish);
        cohort.clearDomainEvents();

        return toDTO(cohort);
    }

    /**
     * Graduates a cohort.
     */
    @Transactional
    public CohortDTO graduateCohort(Long id, Long updatedBy) {
        Cohort cohort = cohortRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        cohort.graduate(updatedBy);
        cohort = cohortRepository.save(cohort);

        // Publish domain events
        cohort.getDomainEvents().forEach(eventPublisher::publish);
        cohort.clearDomainEvents();

        return toDTO(cohort);
    }

    /**
     * Stops enrollment for a cohort.
     */
    @Transactional
    public CohortDTO stopEnrollment(Long id, Long updatedBy) {
        Cohort cohort = cohortRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        cohort.stopEnrollment(updatedBy);
        cohort = cohortRepository.save(cohort);

        // Publish domain events
        cohort.getDomainEvents().forEach(eventPublisher::publish);
        cohort.clearDomainEvents();

        return toDTO(cohort);
    }

    /**
     * Deletes a cohort.
     */
    @Transactional
    public void deleteCohort(Long id) {
        Cohort cohort = cohortRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("年级不存在: " + id));

        // 检查年级下是否有班级
        int classCount = schoolClassRepository.countByGradeId(id);
        if (classCount > 0) {
            throw new IllegalStateException(
                String.format("无法删除年级: 该年级下还有 %d 个班级", classCount));
        }

        cohortRepository.deleteById(id);
    }

    // Helper methods
    private CohortDTO toDTO(Cohort cohort) {
        CohortDTO dto = new CohortDTO();
        dto.setId(cohort.getId());
        dto.setGradeCode(cohort.getGradeCode());
        dto.setGradeName(cohort.getGradeName());
        dto.setEnrollmentYear(cohort.getEnrollmentYear());
        dto.setGraduationYear(cohort.getGraduationYear());
        dto.setSchoolingYears(cohort.getSchoolingYears());
        dto.setDirectorId(cohort.getDirectorId());
        dto.setDirectorName(cohort.getDirectorName());
        dto.setCounselorId(cohort.getCounselorId());
        dto.setCounselorName(cohort.getCounselorName());
        dto.setStandardClassSize(cohort.getStandardClassSize());
        dto.setStatus(cohort.getStatus());
        dto.setStatusDisplayName(cohort.getStatus() != null ? cohort.getStatus().getDisplayName() : null);
        dto.setSortOrder(cohort.getSortOrder());
        dto.setRemarks(cohort.getRemarks());
        dto.setCreatedAt(cohort.getCreatedAt());
        dto.setUpdatedAt(cohort.getUpdatedAt());
        return dto;
    }
}
