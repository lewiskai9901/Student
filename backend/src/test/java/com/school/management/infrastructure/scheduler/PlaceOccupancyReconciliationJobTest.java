package com.school.management.infrastructure.scheduler;

import com.school.management.infrastructure.persistence.place.UniversalPlaceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for {@link PlaceOccupancyReconciliationJob}.
 *
 * <p>Covers the hourly drift-detection job that fixes mismatches between
 * {@code places.current_occupancy} and the actual count derived from
 * {@code place_occupants}. Includes regression coverage for the null-safety
 * fix from commit 9041a474, which prevented NPEs when the mismatch query
 * returned rows with null columns.
 */
@ExtendWith(MockitoExtension.class)
class PlaceOccupancyReconciliationJobTest {

    @Mock
    UniversalPlaceMapper placeMapper;

    @InjectMocks
    PlaceOccupancyReconciliationJob job;

    @Test
    void reconcile_whenMismatchFound_shouldFixCounter() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 100L);
        row.put("storedCount", 5);
        row.put("actualCount", 3);
        when(placeMapper.findOccupancyMismatches()).thenReturn(List.of(row));

        job.reconcile();

        verify(placeMapper).fixOccupancy(100L, 3);
    }

    @Test
    void reconcile_whenNoMismatch_shouldDoNothing() {
        when(placeMapper.findOccupancyMismatches()).thenReturn(List.of());

        job.reconcile();

        verify(placeMapper, never()).fixOccupancy(anyLong(), anyInt());
    }

    @Test
    void reconcile_withNullValuesInRow_shouldSkipGracefully() {
        // Regression for commit 9041a474: null-safe handling of query result fields.
        Map<String, Object> rowWithNulls = new HashMap<>();
        rowWithNulls.put("id", 100L);
        // storedCount and actualCount are missing -> Map.get() returns null
        when(placeMapper.findOccupancyMismatches()).thenReturn(List.of(rowWithNulls));

        // Should NOT throw NPE
        job.reconcile();

        // Should skip this row, not call fixOccupancy
        verify(placeMapper, never()).fixOccupancy(anyLong(), anyInt());
    }

    @Test
    void reconcile_withMultipleMismatches_shouldFixAll() {
        Map<String, Object> row1 = Map.of("id", 100L, "storedCount", 5, "actualCount", 3);
        Map<String, Object> row2 = Map.of("id", 200L, "storedCount", 0, "actualCount", 2);
        when(placeMapper.findOccupancyMismatches()).thenReturn(List.of(row1, row2));

        job.reconcile();

        verify(placeMapper).fixOccupancy(100L, 3);
        verify(placeMapper).fixOccupancy(200L, 2);
    }
}
