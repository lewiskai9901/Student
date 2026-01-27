package com.school.management.interfaces.rest.behavior;

import com.school.management.domain.behavior.model.BehaviorAlert;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BehaviorAlertResponse {
    private Long id;
    private Long studentId;
    private Long classId;
    private String alertType;
    private String alertLevel;
    private String title;
    private String description;
    private String triggerData;
    private boolean isRead;
    private boolean isHandled;
    private Long handledBy;
    private LocalDateTime handledAt;
    private String handleNote;
    private LocalDateTime createdAt;

    public static BehaviorAlertResponse fromDomain(BehaviorAlert alert) {
        BehaviorAlertResponse resp = new BehaviorAlertResponse();
        resp.setId(alert.getId());
        resp.setStudentId(alert.getStudentId());
        resp.setClassId(alert.getClassId());
        resp.setAlertType(alert.getAlertType().name());
        resp.setAlertLevel(alert.getAlertLevel());
        resp.setTitle(alert.getTitle());
        resp.setDescription(alert.getDescription());
        resp.setTriggerData(alert.getTriggerData());
        resp.setRead(alert.isRead());
        resp.setHandled(alert.isHandled());
        resp.setHandledBy(alert.getHandledBy());
        resp.setHandledAt(alert.getHandledAt());
        resp.setHandleNote(alert.getHandleNote());
        resp.setCreatedAt(alert.getCreatedAt());
        return resp;
    }
}
