package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import com.school.management.domain.inspection.model.InspectionDeduction;
import com.school.management.domain.inspection.repository.ClassInspectionRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only query service for the teacher dashboard.
 * Provides aggregated views of class inspection data.
 */
@Service
@Transactional(readOnly = true)
public class TeacherDashboardQueryService {

    private final ClassInspectionRecordRepository classRecordRepository;

    public TeacherDashboardQueryService(ClassInspectionRecordRepository classRecordRepository) {
        this.classRecordRepository = classRecordRepository;
    }

    /**
     * Gets overview statistics for a class within a date range.
     */
    public Map<String, Object> getOverview(Long classId, LocalDate weekStart, LocalDate weekEnd) {
        List<ClassInspectionRecord> records = classRecordRepository.findByClassIdAndDateRange(classId, weekStart, weekEnd);

        BigDecimal weeklyDeduction = records.stream()
            .map(ClassInspectionRecord::getTotalDeduction)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weeklyBonus = records.stream()
            .map(ClassInspectionRecord::getBonusScore)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> overview = new HashMap<>();
        overview.put("classId", classId);
        overview.put("weeklyDeduction", weeklyDeduction);
        overview.put("weeklyBonus", weeklyBonus);
        overview.put("recordCount", records.size());
        return overview;
    }

    /**
     * Gets deduction details for a class within a date range.
     */
    public List<Map<String, Object>> getDeductionDetails(Long classId, LocalDate startDate, LocalDate endDate) {
        List<ClassInspectionRecord> records = classRecordRepository.findByClassIdAndDateRange(classId, startDate, endDate);

        List<Map<String, Object>> details = new ArrayList<>();
        for (ClassInspectionRecord record : records) {
            for (InspectionDeduction deduction : record.getDeductions()) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", deduction.getId());
                detail.put("sessionId", record.getSessionId());
                detail.put("itemName", deduction.getItemName());
                detail.put("categoryName", deduction.getCategoryName());
                detail.put("spaceType", deduction.getSpaceType() != null ? deduction.getSpaceType().name() : null);
                detail.put("spaceName", deduction.getSpaceName());
                detail.put("personCount", deduction.getPersonCount());
                detail.put("deductionAmount", deduction.getDeductionAmount());
                detail.put("remark", deduction.getRemark());
                detail.put("createdAt", deduction.getCreatedAt());
                details.add(detail);
            }
        }
        return details;
    }

    /**
     * Gets top N most frequent issues for a class.
     */
    public List<Map<String, Object>> getTopIssues(Long classId, LocalDate startDate, LocalDate endDate, int topN) {
        List<ClassInspectionRecord> records = classRecordRepository.findByClassIdAndDateRange(classId, startDate, endDate);

        // Group deductions by itemName
        Map<String, List<InspectionDeduction>> byItem = records.stream()
            .flatMap(r -> r.getDeductions().stream())
            .collect(Collectors.groupingBy(InspectionDeduction::getItemName));

        List<Map<String, Object>> issues = new ArrayList<>();
        for (Map.Entry<String, List<InspectionDeduction>> entry : byItem.entrySet()) {
            String itemName = entry.getKey();
            List<InspectionDeduction> deductions = entry.getValue();

            BigDecimal totalDeduction = deductions.stream()
                .map(InspectionDeduction::getDeductionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> issue = new HashMap<>();
            issue.put("issueName", itemName);
            issue.put("occurrenceCount", deductions.size());
            issue.put("totalDeduction", totalDeduction);
            issue.put("categoryName", deductions.get(0).getCategoryName());
            issues.add(issue);
        }

        // Sort by occurrenceCount descending, then by totalDeduction descending
        issues.sort((a, b) -> {
            int cmp = Integer.compare((int) b.get("occurrenceCount"), (int) a.get("occurrenceCount"));
            if (cmp != 0) return cmp;
            return ((BigDecimal) b.get("totalDeduction")).compareTo((BigDecimal) a.get("totalDeduction"));
        });

        return issues.stream().limit(topN).collect(Collectors.toList());
    }

    /**
     * Gets student violation rankings for a class.
     */
    public List<Map<String, Object>> getStudentViolations(Long classId, LocalDate startDate, LocalDate endDate) {
        List<ClassInspectionRecord> records = classRecordRepository.findByClassIdAndDateRange(classId, startDate, endDate);

        // Collect all deductions with student info
        Map<Long, Map<String, Object>> studentMap = new HashMap<>();

        for (ClassInspectionRecord record : records) {
            for (InspectionDeduction deduction : record.getDeductions()) {
                List<Long> studentIds = deduction.getStudentIds();
                List<String> studentNames = deduction.getStudentNames();
                if (studentIds == null || studentIds.isEmpty()) continue;

                for (int i = 0; i < studentIds.size(); i++) {
                    Long studentId = studentIds.get(i);
                    String studentName = i < studentNames.size() ? studentNames.get(i) : "Unknown";

                    Map<String, Object> studentData = studentMap.computeIfAbsent(studentId, id -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("studentId", id);
                        m.put("studentName", studentName);
                        m.put("violationCount", 0);
                        m.put("totalDeduction", BigDecimal.ZERO);
                        m.put("violationTypes", new ArrayList<String>());
                        return m;
                    });

                    studentData.put("violationCount", (int) studentData.get("violationCount") + 1);
                    studentData.put("totalDeduction",
                        ((BigDecimal) studentData.get("totalDeduction")).add(deduction.getDeductionAmount()));
                    @SuppressWarnings("unchecked")
                    List<String> types = (List<String>) studentData.get("violationTypes");
                    if (!types.contains(deduction.getItemName())) {
                        types.add(deduction.getItemName());
                    }
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>(studentMap.values());
        result.sort((a, b) -> Integer.compare((int) b.get("violationCount"), (int) a.get("violationCount")));
        return result;
    }

    /**
     * Gets improvement data comparing two date ranges.
     */
    public Map<String, Object> getImprovement(Long classId,
                                               LocalDate currentStart, LocalDate currentEnd,
                                               LocalDate previousStart, LocalDate previousEnd) {
        List<ClassInspectionRecord> currentRecords = classRecordRepository.findByClassIdAndDateRange(classId, currentStart, currentEnd);
        List<ClassInspectionRecord> previousRecords = classRecordRepository.findByClassIdAndDateRange(classId, previousStart, previousEnd);

        BigDecimal currentDeduction = currentRecords.stream()
            .map(ClassInspectionRecord::getTotalDeduction)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousDeduction = previousRecords.stream()
            .map(ClassInspectionRecord::getTotalDeduction)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal change = currentDeduction.subtract(previousDeduction);
        BigDecimal changePercent = previousDeduction.compareTo(BigDecimal.ZERO) != 0
            ? change.divide(previousDeduction, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        Map<String, Object> improvement = new HashMap<>();
        improvement.put("currentDeduction", currentDeduction);
        improvement.put("previousDeduction", previousDeduction);
        improvement.put("change", change);
        improvement.put("changePercent", changePercent);
        improvement.put("improved", change.compareTo(BigDecimal.ZERO) < 0);
        return improvement;
    }
}
