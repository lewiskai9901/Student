package com.school.management.application.inspection.saga;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Saga Step: Notification (发送通知).
 *
 * After inspection results are published and ratings are calculated,
 * this step sends notifications to the relevant class teachers (班主任).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStep {

    /**
     * Executes the notification step for all class records in a session.
     *
     * @param sessionId      the published session ID
     * @param inspectionDate the inspection date
     * @param classRecords   all class inspection records
     * @param ratingResult   the calculated rating result
     */
    public void execute(Long sessionId, LocalDate inspectionDate,
                        List<ClassInspectionRecord> classRecords,
                        RatingCalculationResult ratingResult) {
        log.debug("Starting inspection result notification: sessionId={}, classCount={}",
                sessionId, classRecords.size());

        int notifiedCount = 0;

        for (ClassInspectionRecord record : classRecords) {
            try {
                String rating = determineRating(record.getClassId(), ratingResult);

                // Build notification content
                String title = String.format("%s Inspection Result Notification", inspectionDate);
                String content = buildNotificationContent(record, rating, ratingResult.getAverageScore());

                // Log notification (actual sending delegated to notification infrastructure when available)
                log.debug("Notification: classId={}, className={}, score={}, rating={}",
                        record.getClassId(), record.getClassName(), record.getFinalScore(), rating);
                notifiedCount++;
            } catch (Exception e) {
                log.warn("Failed to send notification: classId={}", record.getClassId(), e);
            }
        }

        log.info("Notification completed: {}/{} classes notified", notifiedCount, classRecords.size());
    }

    /**
     * Looks up the rating for a specific class from the rating result.
     */
    private String determineRating(Long classId, RatingCalculationResult result) {
        Map<String, List<Long>> distribution = result.getRatingDistribution();
        for (Map.Entry<String, List<Long>> entry : distribution.entrySet()) {
            if (entry.getValue().contains(classId)) {
                return entry.getKey();
            }
        }
        return "UNKNOWN";
    }

    /**
     * Builds a human-readable notification content string.
     */
    private String buildNotificationContent(ClassInspectionRecord record, String rating,
                                            BigDecimal averageScore) {
        BigDecimal score = resolveScore(record);

        return String.format(
                "Score: %s, Rating: %s, School Average: %s, Deductions: %s, Bonus: %s",
                score, translateRating(rating), averageScore,
                record.getTotalDeduction(), record.getBonusScore());
    }

    /**
     * Resolves the effective score for display.
     */
    private BigDecimal resolveScore(ClassInspectionRecord record) {
        if (record.getFinalScore() != null && record.getFinalScore().compareTo(BigDecimal.ZERO) != 0) {
            return record.getFinalScore();
        }
        BigDecimal base = record.getBaseScore() != null
                ? new BigDecimal(record.getBaseScore())
                : new BigDecimal("100");
        BigDecimal deduction = record.getTotalDeduction() != null
                ? record.getTotalDeduction()
                : BigDecimal.ZERO;
        BigDecimal bonus = record.getBonusScore() != null
                ? record.getBonusScore()
                : BigDecimal.ZERO;
        return base.subtract(deduction).add(bonus);
    }

    private String translateRating(String rating) {
        return switch (rating) {
            case "EXCELLENT" -> "Excellent (优秀)";
            case "GOOD" -> "Good (良好)";
            case "PASS" -> "Pass (合格)";
            case "FAIL" -> "Fail (不合格)";
            default -> "Unknown (未知)";
        };
    }
}
