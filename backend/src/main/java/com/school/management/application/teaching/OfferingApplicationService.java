package com.school.management.application.teaching;

import com.school.management.domain.teaching.model.offering.SemesterOffering;
import com.school.management.domain.teaching.model.offering.ClassCourseAssignment;
import com.school.management.domain.teaching.repository.SemesterOfferingRepository;
import com.school.management.domain.teaching.repository.ClassCourseAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class OfferingApplicationService {
    private final SemesterOfferingRepository offeringRepo;
    private final ClassCourseAssignmentRepository assignmentRepo;

    public List<SemesterOffering> listOfferings(Long semesterId) {
        return offeringRepo.findBySemesterId(semesterId);
    }

    public SemesterOffering createOffering(Map<String, Object> data, Long userId) {
        SemesterOffering offering = SemesterOffering.create(
            toLong(data.get("semesterId")),
            toLong(data.get("courseId")),
            (String) data.get("applicableGrade"),
            toInt(data.get("weeklyHours")),
            toInt(data.getOrDefault("startWeek", 1)),
            toInt(data.get("endWeek")),
            userId
        );
        // Apply optional fields
        if (data.containsKey("courseCategory") || data.containsKey("allowCombined")) {
            offering.update(
                toInt(data.get("weeklyHours")),
                toInt(data.getOrDefault("startWeek", 1)),
                toInt(data.get("endWeek")),
                toInt(data.get("courseCategory")),
                toInt(data.get("courseType")),
                toBool(data.get("allowCombined")),
                toInt(data.getOrDefault("maxCombinedClasses", 2)),
                toBool(data.get("allowWalking")),
                (String) data.get("remark")
            );
        }
        return offeringRepo.save(offering);
    }

    public SemesterOffering updateOffering(Long id, Map<String, Object> data) {
        SemesterOffering offering = offeringRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("开课计划不存在: " + id));
        offering.update(
            toInt(data.get("weeklyHours")),
            toInt(data.get("startWeek")),
            toInt(data.get("endWeek")),
            toInt(data.get("courseCategory")),
            toInt(data.get("courseType")),
            toBool(data.get("allowCombined")),
            toInt(data.get("maxCombinedClasses")),
            toBool(data.get("allowWalking")),
            (String) data.get("remark")
        );
        return offeringRepo.save(offering);
    }

    public void deleteOffering(Long id) {
        offeringRepo.deleteById(id);
    }

    public void confirmOffering(Long id) {
        SemesterOffering offering = offeringRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("开课计划不存在: " + id));
        offering.confirm();
        offeringRepo.save(offering);
    }

    // --- Class Assignments ---

    public List<ClassCourseAssignment> listAssignments(Long semesterId, Long orgUnitId) {
        if (orgUnitId != null) {
            return assignmentRepo.findBySemesterIdAndClassId(semesterId, orgUnitId);
        }
        return assignmentRepo.findBySemesterId(semesterId);
    }

    public ClassCourseAssignment createAssignment(Map<String, Object> data) {
        ClassCourseAssignment a = ClassCourseAssignment.create(
            toLong(data.get("semesterId")),
            toLong(data.get("orgUnitId")),
            toLong(data.get("offeringId")),
            toLong(data.get("courseId")),
            toInt(data.get("weeklyHours")),
            toInt(data.get("studentCount"))
        );
        return assignmentRepo.save(a);
    }

    public void deleteAssignment(Long id) {
        assignmentRepo.deleteById(id);
    }

    public void batchConfirmAssignments(Long semesterId, Long orgUnitId) {
        List<ClassCourseAssignment> assignments = assignmentRepo.findBySemesterIdAndClassId(semesterId, orgUnitId);
        for (ClassCourseAssignment a : assignments) {
            a.confirm();
            assignmentRepo.save(a);
        }
    }

    // Helper methods for type conversion from Map
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private Boolean toBool(Object val) {
        if (val == null) return null;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }
}
