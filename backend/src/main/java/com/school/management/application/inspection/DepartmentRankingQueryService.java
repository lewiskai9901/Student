package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import com.school.management.domain.inspection.model.DepartmentResultStats;
import com.school.management.domain.inspection.model.InspectionSession;
import com.school.management.domain.inspection.repository.ClassInspectionRecordRepository;
import com.school.management.domain.inspection.repository.InspectionSessionRepository;
import com.school.management.domain.inspection.service.DepartmentRankingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only query service for department rankings.
 */
@Service
@Transactional(readOnly = true)
public class DepartmentRankingQueryService {

    private final InspectionSessionRepository sessionRepository;
    private final ClassInspectionRecordRepository classRecordRepository;
    private final DepartmentRankingService departmentRankingService;

    public DepartmentRankingQueryService(
            InspectionSessionRepository sessionRepository,
            ClassInspectionRecordRepository classRecordRepository,
            DepartmentRankingService departmentRankingService) {
        this.sessionRepository = sessionRepository;
        this.classRecordRepository = classRecordRepository;
        this.departmentRankingService = departmentRankingService;
    }

    /**
     * Gets department ranking across all published sessions in a date range.
     */
    public List<DepartmentResultStats> getDepartmentRanking(
            LocalDate startDate, LocalDate endDate, BigDecimal classWeight) {

        List<InspectionSession> sessions = sessionRepository.findPublishedByDateRange(startDate, endDate);
        if (sessions.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect all class records from published sessions
        List<ClassInspectionRecord> allRecords = new ArrayList<>();
        for (InspectionSession session : sessions) {
            allRecords.addAll(classRecordRepository.findBySessionId(session.getId()));
        }

        return departmentRankingService.calculateDepartmentRanking(allRecords, classWeight);
    }

    /**
     * Gets department ranking for a single session.
     */
    public List<DepartmentResultStats> getDepartmentRankingBySession(Long sessionId) {
        List<ClassInspectionRecord> records = classRecordRepository.findBySessionId(sessionId);
        return departmentRankingService.calculateDepartmentRanking(records, BigDecimal.ONE);
    }
}
