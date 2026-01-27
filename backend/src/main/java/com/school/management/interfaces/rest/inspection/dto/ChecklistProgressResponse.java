package com.school.management.interfaces.rest.inspection.dto;

import lombok.Data;

@Data
public class ChecklistProgressResponse {
    private int totalClasses;
    private long completedClasses;
    private int totalChecklistResponses;

    public static ChecklistProgressResponse from(java.util.Map<String, Object> progress) {
        ChecklistProgressResponse r = new ChecklistProgressResponse();
        r.setTotalClasses((Integer) progress.get("totalClasses"));
        r.setCompletedClasses((Long) progress.get("completedClasses"));
        r.setTotalChecklistResponses((Integer) progress.get("totalChecklistResponses"));
        return r;
    }
}
