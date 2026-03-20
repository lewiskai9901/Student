package com.school.management.domain.organization.model.entity;

import com.school.management.domain.organization.model.valueobject.AppointmentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 人岗关系实体
 */
public class UserPosition {

    private Long id;
    private Long userId;
    private Long positionId;
    private boolean primary;
    private AppointmentType appointmentType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String appointmentReason;
    private String departureReason;
    private Long tenantId;
    private LocalDateTime createdAt;
    private Long createdBy;

    protected UserPosition() {
    }

    /**
     * 任命
     */
    public static UserPosition appoint(Long userId, Long positionId, boolean isPrimary,
                                        AppointmentType type, LocalDate startDate,
                                        String reason, Long createdBy) {
        UserPosition up = new UserPosition();
        up.userId = Objects.requireNonNull(userId);
        up.positionId = Objects.requireNonNull(positionId);
        up.primary = isPrimary;
        up.appointmentType = type != null ? type : AppointmentType.FORMAL;
        up.startDate = startDate != null ? startDate : LocalDate.now();
        up.appointmentReason = reason;
        up.tenantId = 1L;
        up.createdBy = createdBy;
        up.createdAt = LocalDateTime.now();
        return up;
    }

    /**
     * 从持久化数据重建
     */
    public static UserPosition reconstruct(Long id, Long userId, Long positionId, boolean isPrimary,
                                            AppointmentType appointmentType, LocalDate startDate,
                                            LocalDate endDate, String appointmentReason,
                                            String departureReason, Long tenantId,
                                            LocalDateTime createdAt, Long createdBy) {
        UserPosition up = new UserPosition();
        up.id = id;
        up.userId = userId;
        up.positionId = positionId;
        up.primary = isPrimary;
        up.appointmentType = appointmentType;
        up.startDate = startDate;
        up.endDate = endDate;
        up.appointmentReason = appointmentReason;
        up.departureReason = departureReason;
        up.tenantId = tenantId;
        up.createdAt = createdAt;
        up.createdBy = createdBy;
        return up;
    }

    /**
     * 离任
     */
    public void endAppointment(LocalDate endDate, String reason) {
        this.endDate = endDate != null ? endDate : LocalDate.now();
        this.departureReason = reason;
    }

    /**
     * 是否在任
     */
    public boolean isCurrent() {
        return endDate == null;
    }

    // Getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public Long getPositionId() { return positionId; }
    public boolean isPrimary() { return primary; }
    public AppointmentType getAppointmentType() { return appointmentType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getAppointmentReason() { return appointmentReason; }
    public String getDepartureReason() { return departureReason; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
}
