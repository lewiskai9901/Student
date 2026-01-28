package com.school.management.application.inspection.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportCenterService 测试")
class ExportCenterServiceTest {

    @Mock
    private ExportStrategy deductionStrategy;
    @Mock
    private ExportStrategy ratingStrategy;
    @Mock
    private ExportStrategy statisticsStrategy;

    private ExportCenterService service;

    @BeforeEach
    void setUp() {
        when(deductionStrategy.getScenario()).thenReturn(ExportScenario.DEDUCTION_DETAIL);
        when(ratingStrategy.getScenario()).thenReturn(ExportScenario.RATING_REPORT);
        when(statisticsStrategy.getScenario()).thenReturn(ExportScenario.STATISTICS_REPORT);

        service = new ExportCenterService(List.of(deductionStrategy, ratingStrategy, statisticsStrategy));
    }

    @Nested
    @DisplayName("export 方法")
    class ExportTests {

        @Test
        @DisplayName("应该根据场景分派到正确的策略")
        void shouldDispatchToCorrectStrategy() {
            ExportRequest request = new ExportRequest();
            request.setScenario(ExportScenario.DEDUCTION_DETAIL);
            request.setStartDate(LocalDate.of(2026, 1, 1));
            request.setEndDate(LocalDate.of(2026, 1, 31));

            ExportResult expected = ExportResult.builder()
                    .fileName("test.xlsx")
                    .recordCount(10)
                    .scenario(ExportScenario.DEDUCTION_DETAIL)
                    .generatedAt(LocalDateTime.now())
                    .build();
            when(deductionStrategy.execute(any())).thenReturn(expected);

            ExportResult result = service.export(request);

            assertEquals(expected, result);
            verify(deductionStrategy).execute(request);
            verify(ratingStrategy, never()).execute(any());
            verify(statisticsStrategy, never()).execute(any());
        }

        @Test
        @DisplayName("应该分派到评级报表策略")
        void shouldDispatchToRatingStrategy() {
            ExportRequest request = new ExportRequest();
            request.setScenario(ExportScenario.RATING_REPORT);

            ExportResult expected = ExportResult.builder()
                    .fileName("rating.xlsx")
                    .recordCount(5)
                    .scenario(ExportScenario.RATING_REPORT)
                    .generatedAt(LocalDateTime.now())
                    .build();
            when(ratingStrategy.execute(any())).thenReturn(expected);

            ExportResult result = service.export(request);

            assertEquals(expected, result);
            verify(ratingStrategy).execute(request);
        }

        @Test
        @DisplayName("不支持的场景应该抛出异常")
        void shouldThrowForUnsupportedScenario() {
            // Create service with only one strategy
            ExportCenterService limitedService = new ExportCenterService(List.of(deductionStrategy));

            ExportRequest request = new ExportRequest();
            request.setScenario(ExportScenario.STATISTICS_REPORT);

            assertThrows(IllegalArgumentException.class, () -> limitedService.export(request));
        }
    }

    @Nested
    @DisplayName("estimateCount 方法")
    class EstimateCountTests {

        @Test
        @DisplayName("应该委托给正确的策略")
        void shouldDelegateToCorrectStrategy() {
            ExportRequest request = new ExportRequest();
            request.setScenario(ExportScenario.DEDUCTION_DETAIL);
            when(deductionStrategy.estimateRecordCount(any())).thenReturn(100L);

            long count = service.estimateCount(request);

            assertEquals(100L, count);
            verify(deductionStrategy).estimateRecordCount(request);
        }

        @Test
        @DisplayName("策略不存在时应该返回0")
        void shouldReturnZeroForMissingStrategy() {
            ExportCenterService limitedService = new ExportCenterService(List.of(deductionStrategy));

            ExportRequest request = new ExportRequest();
            request.setScenario(ExportScenario.STATISTICS_REPORT);

            assertEquals(0, limitedService.estimateCount(request));
        }
    }

    @Nested
    @DisplayName("getAvailableScenarios 方法")
    class GetAvailableScenariosTests {

        @Test
        @DisplayName("应该返回所有3种场景")
        void shouldReturnAllScenarios() {
            List<Map<String, String>> scenarios = service.getAvailableScenarios();

            assertEquals(3, scenarios.size());
            assertEquals("DEDUCTION_DETAIL", scenarios.get(0).get("code"));
            assertEquals("扣分明细导出", scenarios.get(0).get("name"));
            assertEquals("RATING_REPORT", scenarios.get(1).get("code"));
            assertEquals("STATISTICS_REPORT", scenarios.get(2).get("code"));
        }

        @Test
        @DisplayName("每个场景应该包含code、name和description")
        void shouldContainAllFields() {
            List<Map<String, String>> scenarios = service.getAvailableScenarios();

            for (Map<String, String> scenario : scenarios) {
                assertNotNull(scenario.get("code"));
                assertNotNull(scenario.get("name"));
                assertNotNull(scenario.get("description"));
            }
        }
    }
}
