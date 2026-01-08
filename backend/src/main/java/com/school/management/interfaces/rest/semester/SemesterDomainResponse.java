package com.school.management.interfaces.rest.semester;

import com.school.management.domain.semester.model.aggregate.Semester;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期领域响应 DTO
 */
@Data
public class SemesterDomainResponse {

    private Long id;
    private String semesterName;
    private String semesterCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer startYear;
    private String semesterType;
    private Boolean isCurrent;
    private String status;
    private Long durationDays;
    private Boolean isOngoing;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从领域模型转换
     */
    public static SemesterDomainResponse fromDomain(Semester semester) {
        if (semester == null) {
            return null;
        }

        SemesterDomainResponse response = new SemesterDomainResponse();
        response.setId(semester.getId());
        response.setSemesterName(semester.getSemesterName());
        response.setSemesterCode(semester.getSemesterCode());
        response.setStartDate(semester.getStartDate());
        response.setEndDate(semester.getEndDate());
        response.setStartYear(semester.getStartYear());
        response.setSemesterType(semester.getSemesterType() != null
                ? semester.getSemesterType().getDescription() : null);
        response.setIsCurrent(semester.getIsCurrent());
        response.setStatus(semester.getStatus() != null
                ? semester.getStatus().getDescription() : null);
        response.setDurationDays(semester.getDurationDays());
        response.setIsOngoing(semester.isOngoing());
        response.setCreatedAt(semester.getCreatedAt());
        response.setUpdatedAt(semester.getUpdatedAt());

        return response;
    }
}
