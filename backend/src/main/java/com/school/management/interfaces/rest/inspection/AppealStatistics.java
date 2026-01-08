package com.school.management.interfaces.rest.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Statistics DTO for appeals.
 */
@Data
@Schema(description = "Appeal statistics")
public class AppealStatistics {

    @Schema(description = "Count by status")
    private Map<String, Long> countByStatus = new HashMap<>();

    public long getTotal() {
        return countByStatus.values().stream().mapToLong(Long::longValue).sum();
    }

    public long getPendingCount() {
        return countByStatus.getOrDefault("PENDING", 0L)
             + countByStatus.getOrDefault("LEVEL1_REVIEWING", 0L)
             + countByStatus.getOrDefault("LEVEL1_APPROVED", 0L)
             + countByStatus.getOrDefault("LEVEL2_REVIEWING", 0L);
    }

    public long getApprovedCount() {
        return countByStatus.getOrDefault("APPROVED", 0L)
             + countByStatus.getOrDefault("EFFECTIVE", 0L);
    }

    public long getRejectedCount() {
        return countByStatus.getOrDefault("LEVEL1_REJECTED", 0L)
             + countByStatus.getOrDefault("REJECTED", 0L);
    }
}
